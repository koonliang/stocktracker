package com.stocktracker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stocktracker.api.ApiException;
import org.junit.jupiter.api.Test;

class NonProdAuthConfigTest {
  @Test
  void normalizesDemoUserSettings() {
    var config = new NonProdAuthConfig();
    config.demoUserMax = 0;
    config.demoUserPrefix = "  ";

    assertEquals(1, config.demoUserMax());
    assertEquals("demo", config.demoUserPrefix());
  }

  @Test
  void preservesNormalizedDemoUserPrefix() {
    var config = new NonProdAuthConfig();
    config.demoUserPrefix = "  Demo-Users ";

    assertEquals("demo-users", config.demoUserPrefix());
  }

  @Test
  void validatesRedirectUriAgainstConfiguredValue() {
    var config = new NonProdAuthConfig();
    config.redirectUri = "http://localhost:5173/auth/callback";

    assertEquals(
        "http://localhost:5173/auth/callback",
        config.requireRedirectUri(" http://localhost:5173/auth/callback "));
    assertThrows(ApiException.class, () -> config.requireRedirectUri("http://evil.test"));
  }

  @Test
  void requiresConfiguredRedirectUri() {
    var config = new NonProdAuthConfig();

    assertThrows(ApiException.class, () -> config.requireRedirectUri("http://localhost"));
  }

  @Test
  void requiresProviderCredentialsAndRedirectUri() {
    var config = new NonProdAuthConfig();
    config.googleClientId = "client";
    config.googleClientSecret = "secret";
    config.redirectUri = "http://localhost";

    config.validateProviderCredentials("google");

    config.facebookClientId = "default";
    config.facebookClientSecret = "secret";
    assertThrows(ApiException.class, () -> config.validateProviderCredentials("facebook"));
  }

  @Test
  void rejectsUnknownProviderAndMissingRedirect() {
    var config = new NonProdAuthConfig();
    config.googleClientId = "client";
    config.googleClientSecret = "secret";

    assertThrows(ApiException.class, () -> config.validateProviderCredentials("google"));
    assertThrows(ApiException.class, () -> config.validateProviderCredentials("github"));
  }
}
