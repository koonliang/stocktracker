package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.AuthCredential;
import com.stocktracker.domain.VerificationToken;
import com.stocktracker.domain.VerificationToken.Purpose;
import com.stocktracker.dto.ForgotPasswordRequest;
import com.stocktracker.dto.LoginRequest;
import com.stocktracker.dto.LoginResponse;
import com.stocktracker.dto.ResendVerificationRequest;
import com.stocktracker.dto.ResetPasswordRequest;
import com.stocktracker.dto.SignUpRequest;
import com.stocktracker.dto.StatusResponse;
import com.stocktracker.dto.UserResponse;
import com.stocktracker.dto.VerifyEmailRequest;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.persistence.VerificationTokenRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/** Dev-mode account flows: sign-in, self-service sign-up, and email verification. */
@ApplicationScoped
public class AuthService {
  private static final Logger LOG = Logger.getLogger(AuthService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  @Inject AppUserRepository users;
  @Inject VerificationTokenRepository tokens;
  @Inject TokenIssuer tokenIssuer;
  @Inject EmailSender emailSender;
  @Inject DevTokenStore devTokenStore;

  @ConfigProperty(name = "stocktracker.auth.verification-token.ttl-seconds", defaultValue = "86400")
  long verificationTtlSeconds;

  @ConfigProperty(name = "stocktracker.auth.reset-token.ttl-seconds", defaultValue = "3600")
  long resetTtlSeconds;

  /**
   * Verifies email+password and returns a signed JWT. Failures are generic (FR-002): an unknown
   * email and a wrong password are indistinguishable. An existing but unverified account is told to
   * verify (403, FR-012).
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    var email = AppUser.normalizeEmail(request.email());
    var user = users.findByNormalizedEmail(email).orElse(null);
    var credential =
        user == null ? null : AuthCredential.<AuthCredential>find("userId", user.id).firstResult();

    if (user == null
        || credential == null
        || request.password() == null
        || !BcryptUtil.matches(request.password(), credential.passwordHash)) {
      LOG.infof("event=login_failed email_present=%s", user != null);
      throw new ApiException(Status.UNAUTHORIZED, "AUTH_FAILED", "Invalid email or password");
    }

    if (user.status != AppUser.Status.ACTIVE) {
      throw new ApiException(
          Status.FORBIDDEN, "EMAIL_UNVERIFIED", "Please verify your email before signing in");
    }

    user.lastLoginAt = LocalDateTime.now();
    LOG.infof("event=login_success user_id=%d", user.id);
    return new LoginResponse(tokenIssuer.issue(user), new UserResponse(user.id, user.email));
  }

  /**
   * Creates an unverified account and issues a verification token (FR-009/011). The response is
   * identical whether the email is new, already pending verification, or already registered
   * (FR-014, non-enumerating). Invalid input fails the password/email policy (FR-010).
   */
  @Transactional
  public StatusResponse signup(SignUpRequest request) {
    if (!PasswordPolicy.isValidEmail(request.email())) {
      throw new ApiException(
          Status.BAD_REQUEST,
          "VALIDATION",
          "Invalid email or password",
          Map.of("rules", "Email format is invalid"));
    }
    var violations = PasswordPolicy.passwordViolations(request.password());
    if (!violations.isEmpty()) {
      throw new ApiException(
          Status.BAD_REQUEST,
          "VALIDATION",
          "Password does not meet the strength policy",
          Map.of("rules", violations));
    }

    var email = AppUser.normalizeEmail(request.email());
    var existing = users.findByNormalizedEmail(email).orElse(null);
    if (existing == null) {
      var user = new AppUser();
      user.email = email;
      user.status = AppUser.Status.UNVERIFIED;
      user.emailVerified = false;
      users.persist(user);
      var credential = new AuthCredential();
      credential.userId = user.id;
      credential.passwordHash = BcryptUtil.bcryptHash(request.password());
      credential.persist();
      issueAndSendVerification(user);
      LOG.infof("event=signup user_id=%d", user.id);
    } else if (existing.status == AppUser.Status.UNVERIFIED) {
      // Pending account re-attempting sign-up: re-issue verification, stay non-enumerating.
      issueAndSendVerification(existing);
    }
    // Already-registered active account: no-op, identical response (FR-014).
    return new StatusResponse("verification_sent");
  }

  /** Activates an account from a valid verification token (FR-013). */
  @Transactional
  public StatusResponse verifyEmail(VerifyEmailRequest request) {
    var token = consumeToken(request.token(), Purpose.EMAIL_VERIFICATION);
    var user = users.findById(token.userId);
    if (user != null && user.status == AppUser.Status.UNVERIFIED) {
      user.status = AppUser.Status.ACTIVE;
      user.emailVerified = true;
      LOG.infof("event=email_verified user_id=%d", user.id);
    }
    return new StatusResponse("verified");
  }

  /** Re-issues verification for a pending account; non-enumerating (FR-012). */
  @Transactional
  public StatusResponse resendVerification(ResendVerificationRequest request) {
    var user = users.findByNormalizedEmail(AppUser.normalizeEmail(request.email())).orElse(null);
    if (user != null && user.status == AppUser.Status.UNVERIFIED) {
      issueAndSendVerification(user);
    }
    return new StatusResponse("verification_sent");
  }

  /**
   * Issues a password-reset token for a sign-in-capable account and emails it (FR-015). The
   * response is identical whether or not the email maps to an account (FR-016, SC-005,
   * non-enumerating).
   */
  @Transactional
  public StatusResponse forgotPassword(ForgotPasswordRequest request) {
    var user = users.findByNormalizedEmail(AppUser.normalizeEmail(request.email())).orElse(null);
    if (user != null && user.status == AppUser.Status.ACTIVE) {
      var rawToken = issueToken(user, Purpose.PASSWORD_RESET, resetTtlSeconds);
      emailSender.sendPasswordReset(user.email, rawToken);
      LOG.infof("event=password_reset_requested user_id=%d", user.id);
    }
    return new StatusResponse("reset_sent");
  }

  /**
   * Sets a new password from a valid reset token, then invalidates existing sessions by stamping
   * {@code sessionsInvalidBefore} so tokens issued before now are rejected (FR-017/018). The new
   * password must satisfy the policy (FR-010). The token is single-use (SC-007).
   */
  @Transactional
  public StatusResponse resetPassword(ResetPasswordRequest request) {
    var violations = PasswordPolicy.passwordViolations(request.newPassword());
    if (!violations.isEmpty()) {
      throw new ApiException(
          Status.BAD_REQUEST,
          "VALIDATION",
          "Password does not meet the strength policy",
          Map.of("rules", violations));
    }
    var token = consumeToken(request.token(), Purpose.PASSWORD_RESET);
    var user = users.findById(token.userId);
    if (user == null) {
      throw new ApiException(
          Status.BAD_REQUEST, "TOKEN_INVALID", "This link is invalid or has expired");
    }
    var credential = AuthCredential.<AuthCredential>find("userId", user.id).firstResult();
    if (credential == null) {
      credential = new AuthCredential();
      credential.userId = user.id;
      credential.persist();
    }
    credential.passwordHash = BcryptUtil.bcryptHash(request.newPassword());
    var invalidBefore = Instant.now();
    user.sessionsInvalidBeforeMs = invalidBefore.toEpochMilli();
    user.sessionsInvalidBefore = LocalDateTime.ofInstant(invalidBefore, ZoneOffset.UTC);
    LOG.infof("event=password_reset user_id=%d", user.id);
    return new StatusResponse("reset");
  }

  private void issueAndSendVerification(AppUser user) {
    var rawToken = issueToken(user, Purpose.EMAIL_VERIFICATION, verificationTtlSeconds);
    emailSender.sendVerification(user.email, rawToken);
  }

  /**
   * Issues a single-use, time-limited token: persists only its SHA-256 hash, supersedes prior
   * unconsumed tokens of the same purpose, and mirrors the raw token into the dev store for the
   * dev-only retrieval endpoint. Returns the raw token (the only place it ever exists in clear).
   */
  String issueToken(AppUser user, Purpose purpose, long ttlSeconds) {
    var now = LocalDateTime.now();
    tokens.supersedePrior(user.id, purpose, now);
    var rawToken = generateRawToken();
    var token = new VerificationToken();
    token.userId = user.id;
    token.purpose = purpose;
    token.tokenHash = sha256(rawToken);
    token.expiresAt = now.plusSeconds(ttlSeconds);
    tokens.persist(token);
    devTokenStore.record(user.email, purpose, rawToken, token.expiresAt);
    return rawToken;
  }

  /** Validates a raw token for the expected purpose and marks it consumed (single-use, SC-007). */
  VerificationToken consumeToken(String rawToken, Purpose purpose) {
    var now = LocalDateTime.now();
    var token = rawToken == null ? null : tokens.findByHash(sha256(rawToken)).orElse(null);
    if (token == null || token.purpose != purpose || !token.isUsable(now)) {
      throw new ApiException(
          Status.BAD_REQUEST, "TOKEN_INVALID", "This link is invalid or has expired");
    }
    token.consumedAt = now;
    return token;
  }

  private static String generateRawToken() {
    var bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String sha256(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
