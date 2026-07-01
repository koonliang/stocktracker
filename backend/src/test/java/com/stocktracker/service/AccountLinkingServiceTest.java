package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.persistence.SocialIdentityRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class AccountLinkingServiceTest {
  private final AppUserRepository users = Mockito.mock(AppUserRepository.class);
  private final SocialIdentityRepository socialIdentities =
      Mockito.mock(SocialIdentityRepository.class);
  private AccountLinkingService service;

  @BeforeEach
  void setUp() {
    service = new AccountLinkingService();
    service.users = users;
    service.socialIdentities = socialIdentities;
  }

  @Test
  void resolveOrLinkReturnsExistingLinkedUser() {
    var identity = new SocialIdentity();
    identity.userId = 4L;
    var user = new AppUser();
    user.id = 4L;
    when(socialIdentities.findByProviderSubject(SocialIdentity.Provider.GOOGLE, "sub"))
        .thenReturn(Optional.of(identity));
    when(users.findById(4L)).thenReturn(user);

    var resolved =
        service.resolveOrLink(SocialIdentity.Provider.GOOGLE, "sub", "user@example.com", true);

    assertEquals(4L, resolved.id);
  }

  @Test
  void resolveOrLinkLinksVerifiedEmailToExistingUserAndActivatesWhenRequested() {
    var user = new AppUser();
    user.id = 7L;
    user.status = AppUser.Status.UNVERIFIED;
    user.emailVerified = false;
    when(socialIdentities.findByProviderSubject(SocialIdentity.Provider.GOOGLE, "sub"))
        .thenReturn(Optional.empty());
    when(users.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(user));

    try (MockedConstruction<SocialIdentity> identities =
        Mockito.mockConstruction(SocialIdentity.class)) {
      var resolved =
          service.resolveOrLink(
              SocialIdentity.Provider.GOOGLE, "sub", "user@example.com", true, true);

      assertEquals(7L, resolved.id);
      assertEquals(AppUser.Status.ACTIVE, user.status);
      assertEquals(true, user.emailVerified);
      var linked = identities.constructed().getFirst();
      assertEquals(7L, linked.userId);
      assertEquals("user@example.com", linked.providerEmail);
      verify(linked).persist();
    }
  }

  @Test
  void resolveOrLinkCreatesSyntheticAccountForUnverifiedEmail() {
    when(socialIdentities.findByProviderSubject(SocialIdentity.Provider.FACEBOOK, "subject-1"))
        .thenReturn(Optional.empty());
    doAnswer(
            invocation -> {
              var user = invocation.<AppUser>getArgument(0);
              user.id = 12L;
              return null;
            })
        .when(users)
        .persist(any(AppUser.class));

    try (MockedConstruction<SocialIdentity> identities =
        Mockito.mockConstruction(SocialIdentity.class)) {
      var created =
          service.resolveOrLink(
              SocialIdentity.Provider.FACEBOOK, "subject-1", "User@Example.com", false);

      assertEquals(12L, created.id);
      assertEquals("facebook-subject-1@federated.local", created.email);
      assertEquals(false, created.emailVerified);
      assertNotNull(identities.constructed().getFirst());
    }
  }

  @Test
  void provisionByEmailNormalizesAndActivatesUser() {
    doAnswer(
            invocation -> {
              var user = invocation.<AppUser>getArgument(0);
              user.id = 22L;
              return null;
            })
        .when(users)
        .persist(any(AppUser.class));

    var user = service.provisionByEmail(" USER@example.com ");

    assertEquals(22L, user.id);
    assertEquals("user@example.com", user.email);
    assertEquals(AppUser.Status.ACTIVE, user.status);
  }
}
