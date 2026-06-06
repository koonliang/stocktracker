package com.stocktracker.security;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.persistence.AppUserRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    if (user == null && email != null && !email.isBlank()) {
      user = users.findByNormalizedEmail(email).orElse(null);
      if (user == null && authMode.isCognito()) {
        user = provision(email);
      }
    }
    if (user == null) {
      return null;
    }
    rejectIfStaleSession(user);
    return user;
  }

  private void rejectIfStaleSession(AppUser user) {
    if (user.sessionsInvalidBefore == null) {
      return;
    }
    long issuedAt = jwt.getIssuedAtTime();
    var issuedInstant = LocalDateTime.ofEpochSecond(issuedAt, 0, ZoneOffset.UTC);
    if (issuedAt > 0 && issuedInstant.isBefore(user.sessionsInvalidBefore)) {
      throw new ApiException(Status.UNAUTHORIZED, "session_expired", "Session is no longer valid");
    }
  }

  @Transactional
  AppUser provision(String email) {
    var user = new AppUser();
    user.email = AppUser.normalizeEmail(email);
    user.status = AppUser.Status.ACTIVE;
    user.emailVerified = true;
    users.persist(user);
    return user;
  }
}
