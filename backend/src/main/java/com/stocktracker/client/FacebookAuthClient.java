package com.stocktracker.client;

import com.stocktracker.api.ApiException;
import com.stocktracker.config.NonProdAuthConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

@ApplicationScoped
public class FacebookAuthClient {
  @Inject NonProdAuthConfig config;

  public GoogleAuthClient.ProviderProfile exchange(String code, String redirectUri) {
    config.validateProviderCredentials("facebook");
    if (code == null || code.isBlank()) {
      throw new ApiException(Status.BAD_REQUEST, "AUTH_FAILED", "Unable to complete sign-in.");
    }
    return GoogleAuthClient.ProviderProfile.fromCode("facebook", code, redirectUri);
  }
}

