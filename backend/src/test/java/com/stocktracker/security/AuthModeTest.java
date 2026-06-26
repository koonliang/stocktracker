package com.stocktracker.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthModeTest {
  @Test
  void resolvesCognitoCaseInsensitively() {
    var authMode = new AuthMode();
    authMode.mode = "  CoGnItO ";

    assertEquals(AuthMode.Mode.COGNITO, authMode.current());
    assertTrue(authMode.isCognito());
    assertFalse(authMode.isDev());
  }

  @Test
  void defaultsUnknownModesToDev() {
    var authMode = new AuthMode();
    authMode.mode = "other";

    assertEquals(AuthMode.Mode.DEV, authMode.current());
    assertTrue(authMode.isNonProduction());
  }
}
