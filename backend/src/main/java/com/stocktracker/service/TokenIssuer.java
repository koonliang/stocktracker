package com.stocktracker.service;

import com.stocktracker.domain.AppUser;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Issues dev-mode RS256 JWTs signed with the bundled private key. Production never calls this —
 * Cognito mints its own tokens. Claims mirror what {@code CurrentUser} validates: {@code sub} (the
 * local user id), {@code email}, and a standard issuer/expiry.
 */
@ApplicationScoped
public class TokenIssuer {
  @ConfigProperty(name = "stocktracker.auth.access-token.ttl-seconds", defaultValue = "3600")
  long ttlSeconds;

  @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "stocktracker-dev")
  String issuer;

  public String issue(AppUser user) {
    var now = Instant.now();
    return Jwt.issuer(issuer)
        .subject(String.valueOf(user.id))
        .upn(user.email)
        .claim("email", user.email)
        .claim("st_iat_ms", now.toEpochMilli())
        .groups(Set.of("user"))
        .expiresIn(Duration.ofSeconds(ttlSeconds))
        .sign();
  }
}
