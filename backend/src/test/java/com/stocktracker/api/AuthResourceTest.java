package com.stocktracker.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.AppUser;
import com.stocktracker.dto.ForgotPasswordRequest;
import com.stocktracker.dto.LoginRequest;
import com.stocktracker.dto.LoginResponse;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCatalogResponse;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCreateRequest;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserListItem;
import com.stocktracker.dto.NonProdAuthDtos.SocialExchangeRequest;
import com.stocktracker.dto.ResendVerificationRequest;
import com.stocktracker.dto.ResetPasswordRequest;
import com.stocktracker.dto.SignUpRequest;
import com.stocktracker.dto.StatusResponse;
import com.stocktracker.dto.UserResponse;
import com.stocktracker.dto.VerifyEmailRequest;
import com.stocktracker.security.AuthMode;
import com.stocktracker.security.CurrentUser;
import com.stocktracker.service.AuthService;
import com.stocktracker.service.DemoUserService;
import com.stocktracker.service.NonProdSocialAuthService;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthResourceTest {
  private final AuthService authService = Mockito.mock(AuthService.class);
  private final CurrentUser currentUser = Mockito.mock(CurrentUser.class);
  private final AuthMode authMode = Mockito.mock(AuthMode.class);
  private final NonProdSocialAuthService nonProdSocialAuthService =
      Mockito.mock(NonProdSocialAuthService.class);
  private final DemoUserService demoUserService = Mockito.mock(DemoUserService.class);

  private AuthResource resource;

  @BeforeEach
  void setUp() {
    resource = new AuthResource();
    resource.authService = authService;
    resource.currentUser = currentUser;
    resource.authMode = authMode;
    resource.nonProdSocialAuthService = nonProdSocialAuthService;
    resource.demoUserService = demoUserService;
  }

  @Test
  void devModeEndpointsReturnExpectedPayloads() {
    when(authMode.isDev()).thenReturn(true);
    when(authService.signup(new SignUpRequest("user@example.com", "StrongPass1!")))
        .thenReturn(new StatusResponse("account_created"));
    when(authService.verifyEmail(new VerifyEmailRequest("verify-token")))
        .thenReturn(new StatusResponse("verified"));
    when(authService.resendVerification(new ResendVerificationRequest("user@example.com")))
        .thenReturn(new StatusResponse("verification_sent"));
    when(authService.login(new LoginRequest("user@example.com", "StrongPass1!")))
        .thenReturn(new LoginResponse("jwt", new UserResponse(1L, "user@example.com")));
    when(nonProdSocialAuthService.exchange("google", new SocialExchangeRequest("code", "uri")))
        .thenReturn(new LoginResponse("social", new UserResponse(2L, "social@example.com")));
    when(demoUserService.catalog())
        .thenReturn(
            new DemoUserCatalogResponse(
                List.of(new DemoUserListItem(1, "Demo User 1", "demo1@example.com")), 3, true));
    var created = demoUser(7L, "demo7@example.com", (byte) 7, "Momentum");
    when(demoUserService.create(new DemoUserCreateRequest("Momentum"))).thenReturn(created);
    when(authService.issueTokenForUser(created)).thenReturn("demo-token");
    when(demoUserService.labelFor(created)).thenReturn("Momentum");
    var loggedIn = demoUser(3L, "demo3@example.com", (byte) 3, "Income");
    when(demoUserService.login(3)).thenReturn(loggedIn);
    when(authService.issueTokenForUser(loggedIn)).thenReturn("demo-login-token");
    when(demoUserService.labelFor(loggedIn)).thenReturn("Income");
    when(authService.forgotPassword(new ForgotPasswordRequest("user@example.com")))
        .thenReturn(new StatusResponse("reset_sent"));
    when(authService.resetPassword(new ResetPasswordRequest("reset-token", "StrongPass1!")))
        .thenReturn(new StatusResponse("password_reset"));
    var me = demoUser(9L, "me@example.com", (byte) 1, "Me");
    when(currentUser.require()).thenReturn(me);

    assertEquals(
        Status.ACCEPTED.getStatusCode(),
        resource.signup(new SignUpRequest("user@example.com", "StrongPass1!")).getStatus());
    assertEquals(
        Status.OK.getStatusCode(),
        resource.verifyEmail(new VerifyEmailRequest("verify-token")).getStatus());
    assertEquals(
        Status.ACCEPTED.getStatusCode(),
        resource.resendVerification(new ResendVerificationRequest("user@example.com")).getStatus());
    assertEquals(
        "jwt", resource.login(new LoginRequest("user@example.com", "StrongPass1!")).token());
    assertEquals(
        "social",
        resource.socialExchange("google", new SocialExchangeRequest("code", "uri")).token());
    assertEquals(1, resource.demoUsers().users().size());

    var createdResponse = resource.createDemoUser(new DemoUserCreateRequest("Momentum"));
    assertEquals(Status.CREATED.getStatusCode(), createdResponse.getStatus());
    assertEquals(
        "demo-token",
        ((com.stocktracker.dto.NonProdAuthDtos.DemoUserLoginResponse) createdResponse.getEntity())
            .token());

    var loginResponse = resource.loginDemoUser(3);
    assertEquals("demo-login-token", loginResponse.token());
    assertEquals("Income", loginResponse.demoUser().label());
    assertEquals(
        Status.ACCEPTED.getStatusCode(),
        resource.forgotPassword(new ForgotPasswordRequest("user@example.com")).getStatus());
    assertEquals(
        Status.OK.getStatusCode(),
        resource
            .resetPassword(new ResetPasswordRequest("reset-token", "StrongPass1!"))
            .getStatus());
    assertEquals("me@example.com", resource.me().email());
    assertEquals(Status.NO_CONTENT.getStatusCode(), resource.logout().getStatus());
  }

  @Test
  void devOnlyEndpointsRejectWhenNotInDevMode() {
    when(authMode.isDev()).thenReturn(false);

    var error =
        assertThrows(
            ApiException.class,
            () -> resource.login(new LoginRequest("user@example.com", "StrongPass1!")));

    assertEquals("not_found", error.code());
  }

  private AppUser demoUser(Long id, String email, byte slot, String label) {
    var user = new AppUser();
    user.id = id;
    user.email = email;
    user.demoSlot = slot;
    user.displayName = label;
    return user;
  }
}
