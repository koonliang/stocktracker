package com.stocktracker.api;

import com.stocktracker.domain.VerificationToken.Purpose;
import com.stocktracker.security.AuthMode;
import com.stocktracker.service.DevTokenStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;

/**
 * Dev-only retrieval of the latest verification/reset token so the e2e suite can drive flows
 * without an inbox (FR-T02). Gated to {@code auth.mode=dev}: in cognito mode every route returns
 * 404 so the endpoint is effectively absent in production.
 */
@Path("/api/dev/auth")
@Produces(MediaType.APPLICATION_JSON)
public class DevAuthTokenResource {
  @Inject AuthMode authMode;
  @Inject DevTokenStore devTokenStore;

  @GET
  @Path("/latest-token")
  public Response latestToken(
      @QueryParam("email") String email, @QueryParam("purpose") String purpose) {
    if (!authMode.isDev()) {
      throw new ApiException(Status.NOT_FOUND, "not_found", "Not found");
    }
    var parsedPurpose = parsePurpose(purpose);
    var entry =
        devTokenStore
            .latest(email == null ? "" : email, parsedPurpose)
            .orElseThrow(() -> new ApiException(Status.NOT_FOUND, "not_found", "No usable token"));
    return Response.ok(
            Map.of(
                "token", entry.token(),
                "purpose", entry.purpose().name(),
                "expiresAt", entry.expiresAt().toString()))
        .build();
  }

  private Purpose parsePurpose(String purpose) {
    try {
      return Purpose.valueOf(purpose == null ? "" : purpose.trim());
    } catch (IllegalArgumentException exception) {
      throw new ApiException(Status.BAD_REQUEST, "VALIDATION", "Unknown token purpose");
    }
  }
}
