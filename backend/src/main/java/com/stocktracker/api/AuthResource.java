package com.stocktracker.api;

import com.stocktracker.dto.LoginRequest;
import com.stocktracker.dto.LoginResponse;
import com.stocktracker.dto.UserResponse;
import com.stocktracker.security.CurrentUser;
import com.stocktracker.service.AuthService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
  @Inject AuthService authService;
  @Inject CurrentUser currentUser;

  @POST
  @Path("/login")
  public LoginResponse login(LoginRequest request) {
    return authService.login(request);
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
}
