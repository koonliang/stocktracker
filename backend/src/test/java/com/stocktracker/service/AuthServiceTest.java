package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.AppUser;
import com.stocktracker.domain.VerificationToken;
import com.stocktracker.domain.VerificationToken.Purpose;
import com.stocktracker.dto.ForgotPasswordRequest;
import com.stocktracker.dto.LoginRequest;
import com.stocktracker.dto.ResendVerificationRequest;
import com.stocktracker.dto.SignUpRequest;
import com.stocktracker.dto.VerifyEmailRequest;
import com.stocktracker.persistence.AppUserRepository;
import com.stocktracker.persistence.VerificationTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class AuthServiceTest {
  private final AppUserRepository users = Mockito.mock(AppUserRepository.class);
  private final VerificationTokenRepository tokens = Mockito.mock(VerificationTokenRepository.class);
  private final TokenIssuer tokenIssuer = Mockito.mock(TokenIssuer.class);
  private final EmailSender emailSender = Mockito.mock(EmailSender.class);
  private final DevTokenStore devTokenStore = Mockito.mock(DevTokenStore.class);

  private AuthService service;

  @BeforeEach
  void setUp() {
    service = Mockito.spy(new AuthService());
    service.users = users;
    service.tokens = tokens;
    service.tokenIssuer = tokenIssuer;
    service.emailSender = emailSender;
    service.devTokenStore = devTokenStore;
    service.verificationTtlSeconds = 3600;
    service.resetTtlSeconds = 1800;
  }

  @Test
  void loginRejectsInvalidCredentials() {
    when(users.findByNormalizedEmail("user@example.com")).thenReturn(Optional.empty());

    var error = assertThrows(ApiException.class, () -> service.login(new LoginRequest("user@example.com", "bad")));

    assertEquals("AUTH_FAILED", error.code());
  }

  @Test
  void signupRejectsInvalidEmailAndWeakPassword() {
    var invalidEmail =
        assertThrows(ApiException.class, () -> service.signup(new SignUpRequest("bad", "StrongPass1!")));
    assertEquals("VALIDATION", invalidEmail.code());

    var weakPassword =
        assertThrows(
            ApiException.class,
            () -> service.signup(new SignUpRequest("user@example.com", "weak")));
    assertEquals("VALIDATION", weakPassword.code());
  }

  @Test
  void signupCreatesCredentialForNewUser() {
    when(users.findByNormalizedEmail("user@example.com")).thenReturn(Optional.empty());
    doAnswer(
            invocation -> {
              var user = invocation.<AppUser>getArgument(0);
              user.id = 11L;
              return null;
            })
        .when(users)
        .persist(any(AppUser.class));

    try (MockedConstruction<com.stocktracker.domain.AuthCredential> ignored =
        Mockito.mockConstruction(com.stocktracker.domain.AuthCredential.class)) {
      var response = service.signup(new SignUpRequest("user@example.com", "StrongPass1!"));

      assertEquals("account_created", response.status());
      verify(users).persist(any(AppUser.class));
    }
  }

  @Test
  void forgotPasswordSendsResetOnlyForActiveUsers() {
    var user = activeUser(9L, "user@example.com");
    when(users.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(user));
    doReturn("reset-token").when(service).issueToken(user, Purpose.PASSWORD_RESET, 1800);

    var response = service.forgotPassword(new ForgotPasswordRequest("user@example.com"));

    assertEquals("reset_sent", response.status());
    verify(emailSender).sendPasswordReset("user@example.com", "reset-token");
  }

  @Test
  void forgotPasswordDoesNothingForMissingOrInactiveUser() {
    when(users.findByNormalizedEmail("missing@example.com")).thenReturn(Optional.empty());
    assertEquals("reset_sent", service.forgotPassword(new ForgotPasswordRequest("missing@example.com")).status());

    var inactive = new AppUser();
    inactive.id = 2L;
    inactive.email = "inactive@example.com";
    inactive.status = AppUser.Status.UNVERIFIED;
    when(users.findByNormalizedEmail("inactive@example.com")).thenReturn(Optional.of(inactive));

    assertEquals("reset_sent", service.forgotPassword(new ForgotPasswordRequest("inactive@example.com")).status());
    verify(emailSender, never()).sendPasswordReset(eq("inactive@example.com"), any());
  }

  @Test
  void resendVerificationIssuesTokenOnlyForUnverifiedUsers() {
    var user = new AppUser();
    user.id = 6L;
    user.email = "user@example.com";
    user.status = AppUser.Status.UNVERIFIED;
    when(users.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(user));
    doReturn("verify-token").when(service).issueToken(user, Purpose.EMAIL_VERIFICATION, 3600);

    var response = service.resendVerification(new ResendVerificationRequest("user@example.com"));

    assertEquals("verification_sent", response.status());
    verify(emailSender).sendVerification("user@example.com", "verify-token");
  }

  @Test
  void resendVerificationDoesNothingForMissingOrActiveUser() {
    when(users.findByNormalizedEmail("missing@example.com")).thenReturn(Optional.empty());
    assertEquals(
        "verification_sent",
        service.resendVerification(new ResendVerificationRequest("missing@example.com")).status());

    var active = activeUser(8L, "active@example.com");
    when(users.findByNormalizedEmail("active@example.com")).thenReturn(Optional.of(active));
    assertEquals(
        "verification_sent",
        service.resendVerification(new ResendVerificationRequest("active@example.com")).status());

    verify(emailSender, never()).sendVerification(eq("active@example.com"), any());
  }

  @Test
  void verifyEmailMarksOnlyUnverifiedUsersActive() {
    var user = new AppUser();
    user.id = 3L;
    user.status = AppUser.Status.UNVERIFIED;
    user.emailVerified = false;
    var token = new VerificationToken();
    token.userId = 3L;
    doReturn(token).when(service).consumeToken("verify-token", Purpose.EMAIL_VERIFICATION);
    when(users.findById(3L)).thenReturn(user);

    var response = service.verifyEmail(new VerifyEmailRequest("verify-token"));

    assertEquals("verified", response.status());
    assertEquals(AppUser.Status.ACTIVE, user.status);
    assertEquals(true, user.emailVerified);
  }

  @Test
  void verifyEmailLeavesMissingOrAlreadyActiveUsersUnchanged() {
    var token = new VerificationToken();
    token.userId = 10L;
    doReturn(token).when(service).consumeToken("verify-token-2", Purpose.EMAIL_VERIFICATION);
    when(users.findById(10L)).thenReturn(null);
    assertEquals("verified", service.verifyEmail(new VerifyEmailRequest("verify-token-2")).status());

    var active = activeUser(11L, "active@example.com");
    token.userId = 11L;
    when(users.findById(11L)).thenReturn(active);
    assertEquals("verified", service.verifyEmail(new VerifyEmailRequest("verify-token-2")).status());
  }

  @Test
  void issueTokenSupersedesPriorAndRecordsDevToken() {
    var user = activeUser(4L, "user@example.com");

    var raw = service.issueToken(user, Purpose.PASSWORD_RESET, 1200);

    verify(tokens).supersedePrior(eq(4L), eq(Purpose.PASSWORD_RESET), any(LocalDateTime.class));
    verify(tokens).persist(any(VerificationToken.class));
    verify(devTokenStore).record(eq("user@example.com"), eq(Purpose.PASSWORD_RESET), eq(raw), any(LocalDateTime.class));
  }

  @Test
  void consumeTokenRejectsWrongPurposeOrExpiredToken() {
    var token = new VerificationToken();
    token.userId = 4L;
    token.purpose = Purpose.EMAIL_VERIFICATION;
    token.expiresAt = LocalDateTime.now().minusMinutes(1);
    when(tokens.findByHash(any())).thenReturn(Optional.of(token));

    var error = assertThrows(ApiException.class, () -> service.consumeToken("raw", Purpose.PASSWORD_RESET));

    assertEquals("TOKEN_INVALID", error.code());
  }

  @Test
  void consumeTokenMarksUsableTokenConsumed() {
    var token = new VerificationToken();
    token.userId = 4L;
    token.purpose = Purpose.PASSWORD_RESET;
    token.expiresAt = LocalDateTime.now().plusMinutes(5);
    when(tokens.findByHash(any())).thenReturn(Optional.of(token));

    var consumed = service.consumeToken("raw", Purpose.PASSWORD_RESET);

    assertEquals(token, consumed);
    org.junit.jupiter.api.Assertions.assertNotNull(token.consumedAt);
  }

  private AppUser activeUser(Long id, String email) {
    var user = new AppUser();
    user.id = id;
    user.email = email;
    user.status = AppUser.Status.ACTIVE;
    user.emailVerified = true;
    return user;
  }
}
