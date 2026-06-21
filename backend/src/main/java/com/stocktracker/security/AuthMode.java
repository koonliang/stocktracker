package com.stocktracker.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves the configured authentication mode. {@code dev} means the backend owns the
 * email+password flows and self-signs tokens; {@code cognito} means Amazon Cognito owns them and
 * the backend only validates pool-issued tokens. Used to gate dev-only beans and endpoints.
 */
@ApplicationScoped
public class AuthMode {
  public enum Mode {
    DEV,
    COGNITO
  }

  @ConfigProperty(name = "stocktracker.auth.mode", defaultValue = "dev")
  String mode;

  public Mode current() {
    return "cognito".equals(mode.trim().toLowerCase(Locale.ROOT)) ? Mode.COGNITO : Mode.DEV;
  }

  public boolean isDev() {
    return current() == Mode.DEV;
  }

  public boolean isCognito() {
    return current() == Mode.COGNITO;
  }

  public boolean isNonProduction() {
    return isDev();
  }
}
