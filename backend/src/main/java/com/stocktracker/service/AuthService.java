package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.AuthCredential;
import com.stocktracker.dto.LoginRequest;
import com.stocktracker.dto.LoginResponse;
import com.stocktracker.dto.UserResponse;
import com.stocktracker.persistence.AppUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/** Dev-mode account flows. Sign-up/verify/reset are layered on in later user stories. */
@ApplicationScoped
public class AuthService {
  private static final Logger LOG = Logger.getLogger(AuthService.class);

  @Inject AppUserRepository users;
  @Inject TokenIssuer tokenIssuer;

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
}
