package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.client.FacebookAuthClient;
import com.stocktracker.client.GoogleAuthClient;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.dto.LoginResponse;
import com.stocktracker.dto.NonProdAuthDtos.SocialExchangeRequest;
import com.stocktracker.dto.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.util.Locale;

@ApplicationScoped
public class NonProdSocialAuthService {
  @Inject GoogleAuthClient googleAuthClient;
  @Inject FacebookAuthClient facebookAuthClient;
  @Inject AccountLinkingService accountLinkingService;
  @Inject TokenIssuer tokenIssuer;

  @Transactional
  public LoginResponse exchange(String provider, SocialExchangeRequest request) {
    if (request == null || request.code() == null || request.code().isBlank()) {
      throw new ApiException(Status.BAD_REQUEST, "AUTH_FAILED", "Unable to complete sign-in.");
    }

    var normalizedProvider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    GoogleAuthClient.ProviderProfile profile =
        switch (normalizedProvider) {
          case "google" -> googleAuthClient.exchange(request.code(), request.redirectUri());
          case "facebook" -> facebookAuthClient.exchange(request.code(), request.redirectUri());
          default -> throw new ApiException(Status.NOT_FOUND, "not_found", "Not found");
        };

    var user =
        accountLinkingService.resolveOrLink(
            "google".equals(normalizedProvider)
                ? SocialIdentity.Provider.GOOGLE
                : SocialIdentity.Provider.FACEBOOK,
            profile.subject(),
            profile.email(),
            profile.emailVerified(),
            true);
    user.status = com.stocktracker.domain.AppUser.Status.ACTIVE;
    user.emailVerified = true;
    return new LoginResponse(tokenIssuer.issue(user), new UserResponse(user.id, user.email));
  }
}
