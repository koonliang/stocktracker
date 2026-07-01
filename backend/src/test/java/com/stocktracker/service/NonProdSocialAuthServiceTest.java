package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.client.FacebookAuthClient;
import com.stocktracker.client.GoogleAuthClient;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.dto.NonProdAuthDtos.SocialExchangeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NonProdSocialAuthServiceTest {
  private final GoogleAuthClient googleAuthClient = Mockito.mock(GoogleAuthClient.class);
  private final FacebookAuthClient facebookAuthClient = Mockito.mock(FacebookAuthClient.class);
  private final AccountLinkingService accountLinkingService =
      Mockito.mock(AccountLinkingService.class);
  private final TokenIssuer tokenIssuer = Mockito.mock(TokenIssuer.class);

  private NonProdSocialAuthService service;

  @BeforeEach
  void setUp() {
    service = new NonProdSocialAuthService();
    service.googleAuthClient = googleAuthClient;
    service.facebookAuthClient = facebookAuthClient;
    service.accountLinkingService = accountLinkingService;
    service.tokenIssuer = tokenIssuer;
  }

  @Test
  void exchangeRejectsMissingCodeAndUnknownProvider() {
    var missing =
        assertThrows(
            ApiException.class,
            () ->
                service.exchange("google", new SocialExchangeRequest(" ", "https://app/callback")));
    assertEquals("AUTH_FAILED", missing.code());

    var unknown =
        assertThrows(
            ApiException.class,
            () -> service.exchange("x", new SocialExchangeRequest("code", "https://app/callback")));
    assertEquals("not_found", unknown.code());
  }

  @Test
  void exchangeGoogleActivatesUserAndIssuesToken() {
    var profile = new GoogleAuthClient.ProviderProfile("sub-1", "user@example.com", true);
    var user = new AppUser();
    user.id = 7L;
    user.email = "user@example.com";
    when(googleAuthClient.exchange("code", "https://app/callback")).thenReturn(profile);
    when(accountLinkingService.resolveOrLink(
            SocialIdentity.Provider.GOOGLE, "sub-1", "user@example.com", true, true))
        .thenReturn(user);
    when(tokenIssuer.issue(user)).thenReturn("jwt");

    var response =
        service.exchange(" google ", new SocialExchangeRequest("code", "https://app/callback"));

    assertEquals("jwt", response.token());
    assertEquals(7L, response.user().id());
    assertEquals(AppUser.Status.ACTIVE, user.status);
    assertEquals(true, user.emailVerified);
  }

  @Test
  void exchangeFacebookUsesFacebookProviderMapping() {
    var profile = new GoogleAuthClient.ProviderProfile("fb-sub", "fb@example.com", false);
    var user = new AppUser();
    user.id = 9L;
    user.email = "fb@example.com";
    when(facebookAuthClient.exchange("code", "https://app/callback")).thenReturn(profile);
    when(accountLinkingService.resolveOrLink(
            SocialIdentity.Provider.FACEBOOK, "fb-sub", "fb@example.com", false, true))
        .thenReturn(user);
    when(tokenIssuer.issue(user)).thenReturn("jwt-fb");

    var response =
        service.exchange("facebook", new SocialExchangeRequest("code", "https://app/callback"));

    assertEquals("jwt-fb", response.token());
    verify(facebookAuthClient).exchange("code", "https://app/callback");
  }
}
