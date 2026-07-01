package com.stocktracker.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.service.AccountLinkingService;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import java.time.LocalDateTime;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CurrentUserTest {
  private final JsonWebToken jwt = Mockito.mock(JsonWebToken.class);
  private final AppUserRepository users = Mockito.mock(AppUserRepository.class);
  private final AuthMode authMode = Mockito.mock(AuthMode.class);
  private final AccountLinkingService accountLinking = Mockito.mock(AccountLinkingService.class);

  private CurrentUser currentUser;

  @BeforeEach
  void setUp() {
    currentUser = new CurrentUser();
    currentUser.jwt = jwt;
    currentUser.users = users;
    currentUser.authMode = authMode;
    currentUser.accountLinking = accountLinking;
  }

  @Test
  void optionalCachesResolvedUser() {
    var user = user(42L, "user@example.com");
    when(jwt.getSubject()).thenReturn("42");
    when(users.findById(42L)).thenReturn(user);

    var first = currentUser.optional();
    var second = currentUser.optional();

    assertEquals(user, first.orElseThrow());
    assertEquals(user, second.orElseThrow());
    verify(users).findById(42L);
  }

  @Test
  void requireThrowsWhenNoUserCanBeResolved() {
    when(jwt.getSubject()).thenReturn(null);
    when(jwt.getClaim("email")).thenReturn(null);
    when(authMode.isCognito()).thenReturn(false);

    var error = assertThrows(ApiException.class, () -> currentUser.require());

    assertEquals("unauthenticated", error.code());
  }

  @Test
  void resolvesFederatedCognitoUserAndParsesStringEmailVerified() {
    var linked = user(7L, "federated@example.com");
    when(jwt.getSubject()).thenReturn(null);
    when(jwt.getClaim("email")).thenReturn("federated@example.com");
    when(authMode.isCognito()).thenReturn(true);
    Mockito.doReturn(
            Json.createArrayBuilder()
                .add(
                    Json.createObjectBuilder()
                        .add("providerName", "google")
                        .add("userId", "google-subject")
                        .build())
                .build())
        .when(jwt)
        .getClaim("identities");
    Mockito.doReturn(Json.createValue("true")).when(jwt).getClaim("email_verified");
    when(accountLinking.resolveOrLink(
            SocialIdentity.Provider.GOOGLE, "google-subject", "federated@example.com", true))
        .thenReturn(linked);

    var resolved = currentUser.require();

    assertEquals(linked, resolved);
  }

  @Test
  void provisionsCognitoEmailUserWhenFederatedIdentityMissing() {
    var provisioned = user(8L, "new@example.com");
    when(jwt.getSubject()).thenReturn(null);
    when(authMode.isCognito()).thenReturn(true);
    when(jwt.getClaim("email")).thenReturn("new@example.com");
    Mockito.doReturn(null).when(jwt).getClaim("identities");
    when(users.findByNormalizedEmail("new@example.com")).thenReturn(Optional.empty());
    when(accountLinking.provisionByEmail("new@example.com")).thenReturn(provisioned);

    var resolved = currentUser.require();

    assertEquals(provisioned, resolved);
  }

  @Test
  void resolvesNonCognitoUserByEmail() {
    var user = user(9L, "plain@example.com");
    when(jwt.getSubject()).thenReturn("not-a-number");
    when(authMode.isCognito()).thenReturn(false);
    when(jwt.getClaim("email")).thenReturn("plain@example.com");
    when(users.findByNormalizedEmail("plain@example.com")).thenReturn(Optional.of(user));

    assertEquals(9L, currentUser.id());
    verify(accountLinking, never()).provisionByEmail("plain@example.com");
  }

  @Test
  void rejectsStaleSessionUsingMillisClaimString() {
    var user = user(12L, "stale@example.com");
    user.sessionsInvalidBeforeMs = 1_700_000_000_000L;
    when(jwt.getSubject()).thenReturn("12");
    when(users.findById(12L)).thenReturn(user);
    when(jwt.getClaim("st_iat_ms")).thenReturn("1700000000000");

    var error = assertThrows(ApiException.class, () -> currentUser.require());

    assertEquals("session_expired", error.code());
  }

  @Test
  void rejectsStaleSessionUsingIssuedAtSecondsFallback() {
    var user = user(15L, "legacy@example.com");
    user.sessionsInvalidBefore = LocalDateTime.of(2026, 6, 26, 0, 0, 0);
    when(jwt.getSubject()).thenReturn("15");
    when(users.findById(15L)).thenReturn(user);
    when(jwt.getClaim("st_iat_ms")).thenReturn("bad-value");
    when(jwt.getIssuedAtTime()).thenReturn(1_782_432_000L);

    var error = assertThrows(ApiException.class, () -> currentUser.require());

    assertEquals("session_expired", error.code());
  }

  @Test
  void toleratesUnknownFederatedProviderAndMissingEmailVerification() {
    var user = user(21L, "fallback@example.com");
    when(jwt.getSubject()).thenReturn(null);
    when(authMode.isCognito()).thenReturn(true);
    when(jwt.getClaim("email")).thenReturn("fallback@example.com");
    Mockito.doReturn(
            Json.createArrayBuilder()
                .add(
                    Json.createObjectBuilder()
                        .add("providerName", "linkedin")
                        .add("userId", "x")
                        .build())
                .build())
        .when(jwt)
        .getClaim("identities");
    Mockito.doReturn((JsonValue) null).when(jwt).getClaim("email_verified");
    when(users.findByNormalizedEmail("fallback@example.com")).thenReturn(Optional.of(user));

    var resolved = currentUser.optional();

    assertTrue(resolved.isPresent());
    assertEquals(user, resolved.orElseThrow());
  }

  @Test
  void returnsEmptyOptionalWhenTokenHasNoUsableIdentity() {
    when(jwt.getSubject()).thenReturn(null);
    when(authMode.isCognito()).thenReturn(true);
    when(jwt.getClaim("email")).thenReturn("   ");
    Mockito.doReturn(Json.createArrayBuilder().build()).when(jwt).getClaim("identities");

    var resolved = currentUser.optional();

    assertFalse(resolved.isPresent());
  }

  @Test
  void acceptsFreshSessionWithNumericMillisClaim() {
    var user = user(30L, "fresh@example.com");
    user.sessionsInvalidBeforeMs = 1_700_000_000_000L;
    when(jwt.getSubject()).thenReturn("30");
    when(users.findById(30L)).thenReturn(user);
    when(jwt.getClaim("st_iat_ms")).thenReturn(1_700_000_000_001L);

    var resolved = currentUser.require();

    assertNotNull(resolved);
    assertEquals(30L, resolved.id);
  }

  private AppUser user(Long id, String email) {
    var user = new AppUser();
    user.id = id;
    user.email = email;
    return user;
  }
}
