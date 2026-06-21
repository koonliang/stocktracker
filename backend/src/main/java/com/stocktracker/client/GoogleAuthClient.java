package com.stocktracker.client;

import com.stocktracker.api.ApiException;
import com.stocktracker.config.NonProdAuthConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;

@ApplicationScoped
public class GoogleAuthClient {
  @Inject NonProdAuthConfig config;

  public ProviderProfile exchange(String code, String redirectUri) {
    config.validateProviderCredentials("google");
    if (code == null || code.isBlank()) {
      throw new ApiException(Status.BAD_REQUEST, "AUTH_FAILED", "Unable to complete sign-in.");
    }
    return ProviderProfile.fromCode("google", code, redirectUri);
  }

  public record ProviderProfile(String subject, String email, boolean emailVerified) {
    static ProviderProfile fromCode(String provider, String code, String redirectUri) {
      try {
        var parts = Map.of("provider", provider, "code", code, "redirectUri", redirectUri);
        var subject = "%s-%s".formatted(provider, sanitize(parts.get("code")));
        var email = "%s@social.local".formatted(sanitize(parts.get("code")));
        return new ProviderProfile(subject, email, true);
      } catch (Exception exception) {
        throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Unable to complete sign-in.");
      }
    }

    private static String sanitize(String value) {
      var sanitized = value == null ? "" : value.trim().toLowerCase();
      return sanitized.replaceAll("[^a-z0-9]+", "-");
    }
  }
}

