package com.stocktracker.config;

import com.stocktracker.api.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response.Status;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class NonProdAuthConfig {
  @ConfigProperty(name = "nonprod.google.client-id", defaultValue = "")
  String googleClientId;

  @ConfigProperty(name = "nonprod.google.client-secret", defaultValue = "")
  String googleClientSecret;

  @ConfigProperty(name = "nonprod.facebook.client-id", defaultValue = "")
  String facebookClientId;

  @ConfigProperty(name = "nonprod.facebook.client-secret", defaultValue = "")
  String facebookClientSecret;

  @ConfigProperty(name = "nonprod.social.redirect-uri", defaultValue = "")
  String redirectUri;

  @ConfigProperty(name = "stocktracker.demo-users.enabled", defaultValue = "true")
  boolean demoUsersEnabled;

  @ConfigProperty(name = "stocktracker.demo-user.max", defaultValue = "3")
  int demoUserMax;

  @ConfigProperty(name = "stocktracker.demo-user.prefix", defaultValue = "demo")
  String demoUserPrefix;

  public String googleClientId() {
    return googleClientId;
  }

  public String googleClientSecret() {
    return googleClientSecret;
  }

  public String facebookClientId() {
    return facebookClientId;
  }

  public String facebookClientSecret() {
    return facebookClientSecret;
  }

  public String redirectUri() {
    return redirectUri;
  }

  public String requireRedirectUri(String candidateRedirectUri) {
    if (!isConfigured(redirectUri)) {
      throw new ApiException(
          Status.BAD_REQUEST,
          "AUTH_NOT_CONFIGURED",
          "Non-production social sign-in is not configured.");
    }

    var expected = redirectUri.trim();
    var actual = candidateRedirectUri == null ? "" : candidateRedirectUri.trim();
    if (!expected.equals(actual)) {
      throw new ApiException(Status.BAD_REQUEST, "AUTH_FAILED", "Unable to complete sign-in.");
    }
    return expected;
  }

  public boolean demoUsersEnabled() {
    return demoUsersEnabled;
  }

  public int demoUserMax() {
    return Math.max(demoUserMax, 1);
  }

  public String demoUserPrefix() {
    var normalized =
        demoUserPrefix == null ? "demo" : demoUserPrefix.trim().toLowerCase(Locale.ROOT);
    return normalized.isBlank() ? "demo" : normalized;
  }

  public void validateProviderCredentials(String provider) {
    var configured =
        switch (provider.toLowerCase(Locale.ROOT)) {
          case "google" -> isConfigured(googleClientId) && isConfigured(googleClientSecret);
          case "facebook" -> isConfigured(facebookClientId) && isConfigured(facebookClientSecret);
          default -> false;
        };
    if (!configured || !isConfigured(redirectUri)) {
      throw new ApiException(
          Status.BAD_REQUEST,
          "AUTH_NOT_CONFIGURED",
          "Non-production social sign-in is not configured.");
    }
  }

  private boolean isConfigured(String value) {
    if (value == null) {
      return false;
    }
    var normalized = value.trim();
    return !normalized.isBlank() && !"default".equalsIgnoreCase(normalized);
  }
}
