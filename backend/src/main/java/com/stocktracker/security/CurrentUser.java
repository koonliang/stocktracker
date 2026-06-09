package com.stocktracker.security;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.SocialIdentity;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.service.AccountLinkingService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.Response.Status;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Resolves the {@link AppUser} behind the validated bearer token, independent of auth mode. The
 * token's {@code sub} (local user id) is tried first, then the {@code email} claim. On a first
 * Cognito-issued token the matching account is provisioned just-in-time. Tokens issued before the
 * user's {@code sessionsInvalidBefore} instant are rejected (FR-018).
 */
@RequestScoped
public class CurrentUser {
  @Inject JsonWebToken jwt;
  @Inject AppUserRepository users;
  @Inject AuthMode authMode;
  @Inject AccountLinkingService accountLinking;

  private boolean resolved;
  private AppUser cached;

  /** The current user, or empty when the request carries no usable token. */
  public Optional<AppUser> optional() {
    if (resolved) {
      return Optional.ofNullable(cached);
    }
    resolved = true;
    cached = resolve();
    return Optional.ofNullable(cached);
  }

  /** The current user; throws 401 when unauthenticated (use on protected paths). */
  public AppUser require() {
    return optional()
        .orElseThrow(
            () -> new ApiException(Status.UNAUTHORIZED, "unauthenticated", "Not signed in"));
  }

  public Long id() {
    return require().id;
  }

  private AppUser resolve() {
    var subject = jwt.getSubject();
    AppUser user = null;
    if (subject != null && subject.matches("\\d+")) {
      user = users.findById(Long.valueOf(subject));
    }
    String email = jwt.getClaim("email");
    if (user == null && authMode.isCognito()) {
      // Federated (Google/Facebook) tokens are resolved/linked by provider subject (FR-S03/S04);
      // a non-federated Cognito token falls back to email-keyed JIT provisioning.
      var federated = federatedIdentity();
      if (federated != null) {
        user =
            accountLinking.resolveOrLink(
                federated.provider, federated.subject, email, emailVerified());
      } else if (email != null && !email.isBlank()) {
        user = users.findByNormalizedEmail(email).orElse(null);
        if (user == null) {
          // Through the injected proxy so @Transactional applies (self-invocation would not).
          user = accountLinking.provisionByEmail(email);
        }
      }
    } else if (user == null && email != null && !email.isBlank()) {
      user = users.findByNormalizedEmail(email).orElse(null);
    }
    if (user == null) {
      return null;
    }
    rejectIfStaleSession(user);
    return user;
  }

  private record FederatedIdentity(SocialIdentity.Provider provider, String subject) {}

  /**
   * Reads the Cognito {@code identities} claim (present on federated sign-ins) and maps it to a
   * provider + provider-subject. Returns {@code null} for non-federated tokens.
   */
  private FederatedIdentity federatedIdentity() {
    var claim = jwt.<JsonValue>getClaim("identities");
    if (claim == null || claim.getValueType() != JsonValue.ValueType.ARRAY) {
      return null;
    }
    var array = (JsonArray) claim;
    if (array.isEmpty()) {
      return null;
    }
    var entry = array.getJsonObject(0);
    var provider = mapProvider(entry.getString("providerName", null));
    var subject = entry.getString("userId", null);
    if (provider == null || subject == null || subject.isBlank()) {
      return null;
    }
    return new FederatedIdentity(provider, subject);
  }

  private SocialIdentity.Provider mapProvider(String providerName) {
    if (providerName == null) {
      return null;
    }
    return switch (providerName.toUpperCase(Locale.ROOT)) {
      case "GOOGLE" -> SocialIdentity.Provider.GOOGLE;
      case "FACEBOOK" -> SocialIdentity.Provider.FACEBOOK;
      default -> null;
    };
  }

  /** Cognito sends {@code email_verified} as a boolean or a string; accept either. */
  private boolean emailVerified() {
    var claim = jwt.<JsonValue>getClaim("email_verified");
    if (claim == null) {
      return false;
    }
    if (claim.getValueType() == JsonValue.ValueType.TRUE) {
      return true;
    }
    if (claim instanceof jakarta.json.JsonString jsonString) {
      return Boolean.parseBoolean(jsonString.getString());
    }
    return false;
  }

  private void rejectIfStaleSession(AppUser user) {
    if (user.sessionsInvalidBeforeMs == null && user.sessionsInvalidBefore == null) {
      return;
    }
    var issuedAtMillis = issuedAtMillis();
    if (issuedAtMillis != null && user.sessionsInvalidBeforeMs != null) {
      if (issuedAtMillis <= user.sessionsInvalidBeforeMs) {
        throw new ApiException(
            Status.UNAUTHORIZED, "session_expired", "Session is no longer valid");
      }
      return;
    }
    var issuedAt = issuedAt();
    if (issuedAt != null
        && user.sessionsInvalidBefore != null
        && !issuedAt.isAfter(user.sessionsInvalidBefore)) {
      throw new ApiException(Status.UNAUTHORIZED, "session_expired", "Session is no longer valid");
    }
  }

  private LocalDateTime issuedAt() {
    var issuedAtMillis = issuedAtMillis();
    if (issuedAtMillis != null && issuedAtMillis > 0) {
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(issuedAtMillis), ZoneOffset.UTC);
    }
    long issuedAtSeconds = jwt.getIssuedAtTime();
    if (issuedAtSeconds <= 0) {
      return null;
    }
    return LocalDateTime.ofEpochSecond(issuedAtSeconds, 0, ZoneOffset.UTC);
  }

  private Long issuedAtMillis() {
    var issuedAtMillis = issuedAtMillisClaim();
    if (issuedAtMillis != null && issuedAtMillis > 0) {
      return issuedAtMillis;
    }
    long issuedAtSeconds = jwt.getIssuedAtTime();
    if (issuedAtSeconds <= 0) {
      return null;
    }
    return issuedAtSeconds * 1000;
  }

  private Long issuedAtMillisClaim() {
    var claim = jwt.getClaim("st_iat_ms");
    if (claim instanceof Number number) {
      return number.longValue();
    }
    if (claim instanceof String value) {
      try {
        return Long.parseLong(value);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }
}
