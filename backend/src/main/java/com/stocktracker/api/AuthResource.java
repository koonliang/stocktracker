package com.stocktracker.api;

import com.stocktracker.dto.ForgotPasswordRequest;
import com.stocktracker.dto.LoginRequest;
import com.stocktracker.dto.LoginResponse;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCatalogResponse;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserCreateRequest;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserLoginResponse;
import com.stocktracker.dto.NonProdAuthDtos.DemoUserSummary;
import com.stocktracker.dto.NonProdAuthDtos.SocialExchangeRequest;
import com.stocktracker.dto.ResendVerificationRequest;
import com.stocktracker.dto.ResetPasswordRequest;
import com.stocktracker.dto.SignUpRequest;
import com.stocktracker.dto.UserResponse;
import com.stocktracker.dto.VerifyEmailRequest;
import com.stocktracker.security.AuthMode;
import com.stocktracker.security.CurrentUser;
import com.stocktracker.service.AuthService;
import com.stocktracker.service.DemoUserService;
import com.stocktracker.service.NonProdSocialAuthService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
  @Inject AuthService authService;
  @Inject CurrentUser currentUser;
  @Inject AuthMode authMode;
  @Inject NonProdSocialAuthService nonProdSocialAuthService;
  @Inject DemoUserService demoUserService;

  @POST
  @Path("/signup")
  public Response signup(SignUpRequest request) {
    assertDevMode();
    return Response.accepted(authService.signup(request)).build();
  }

  @POST
  @Path("/verify-email")
  public Response verifyEmail(VerifyEmailRequest request) {
    assertDevMode();
    return Response.ok(authService.verifyEmail(request)).build();
  }

  @POST
  @Path("/resend-verification")
  public Response resendVerification(ResendVerificationRequest request) {
    assertDevMode();
    return Response.accepted(authService.resendVerification(request)).build();
  }

  @POST
  @Path("/login")
  public LoginResponse login(LoginRequest request) {
    assertDevMode();
    return authService.login(request);
  }

  @POST
  @Path("/social/{provider}/exchange")
  public LoginResponse socialExchange(
      @jakarta.ws.rs.PathParam("provider") String provider, SocialExchangeRequest request) {
    assertDevMode();
    return nonProdSocialAuthService.exchange(provider, request);
  }

  @GET
  @Path("/demo-users")
  public DemoUserCatalogResponse demoUsers() {
    assertDevMode();
    return demoUserService.catalog();
  }

  @POST
  @Path("/demo-users")
  public Response createDemoUser(DemoUserCreateRequest request) {
    assertDevMode();
    var user = demoUserService.create(request);
    var token = authService.issueTokenForUser(user);
    return Response.status(Status.CREATED)
        .entity(
            new DemoUserLoginResponse(
                token,
                new UserResponse(user.id, user.email),
                new DemoUserSummary(user.demoSlot.intValue(), demoUserService.labelFor(user))))
        .build();
  }

  @POST
  @Path("/demo-users/{slot}/login")
  public DemoUserLoginResponse loginDemoUser(@jakarta.ws.rs.PathParam("slot") int slot) {
    assertDevMode();
    var user = demoUserService.login(slot);
    var token = authService.issueTokenForUser(user);
    return new DemoUserLoginResponse(
        token,
        new UserResponse(user.id, user.email),
        new DemoUserSummary(user.demoSlot.intValue(), demoUserService.labelFor(user)));
  }

  @POST
  @Path("/forgot-password")
  public Response forgotPassword(ForgotPasswordRequest request) {
    assertDevMode();
    return Response.accepted(authService.forgotPassword(request)).build();
  }

  @POST
  @Path("/reset-password")
  public Response resetPassword(ResetPasswordRequest request) {
    assertDevMode();
    return Response.ok(authService.resetPassword(request)).build();
  }

  @GET
  @Path("/me")
  @Authenticated
  public UserResponse me() {
    var user = currentUser.require();
    return new UserResponse(user.id, user.email);
  }

  @POST
  @Path("/logout")
  public Response logout() {
    // Stateless JWT: sign-out is a client-side token discard. Acknowledge only.
    return Response.noContent().build();
  }

  /**
   * The local email+password flows exist only in dev mode. In cognito mode Cognito owns sign-up,
   * verification, login, and reset, so these endpoints return 404 — they must never create local
   * password accounts that bypass the pool (FR-T02).
   */
  private void assertDevMode() {
    if (!authMode.isDev()) {
      throw new ApiException(Status.NOT_FOUND, "not_found", "Not found");
    }
  }
}
