package com.stocktracker.api;

import com.stocktracker.scheduler.FxRefreshJob;
import com.stocktracker.scheduler.QuoteRefreshJob;
import com.stocktracker.scheduler.TokenCleanupJob;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Internal batch-job entrypoints for EventBridge-triggered production invocations. */
@ApplicationScoped
@Path("/api/internal/jobs")
public class InternalJobsResource {
  private static final String TOKEN_HEADER = "x-stocktracker-scheduler-token";

  @Inject QuoteRefreshJob quoteRefreshJob;
  @Inject TokenCleanupJob tokenCleanupJob;
  @Inject FxRefreshJob fxRefreshJob;

  @ConfigProperty(name = "stocktracker.scheduler.token")
  Optional<String> schedulerToken;

  @POST
  @Path("/quote-refresh")
  public Response quoteRefresh(@HeaderParam(TOKEN_HEADER) String token) {
    requireSchedulerToken(token);
    quoteRefreshJob.refresh();
    return Response.accepted().build();
  }

  @POST
  @Path("/token-cleanup")
  public Response tokenCleanup(@HeaderParam(TOKEN_HEADER) String token) {
    requireSchedulerToken(token);
    tokenCleanupJob.purge();
    return Response.accepted().build();
  }

  @POST
  @Path("/fx-refresh")
  public Response fxRefresh(@HeaderParam(TOKEN_HEADER) String token) {
    requireSchedulerToken(token);
    fxRefreshJob.refresh();
    return Response.accepted().build();
  }

  private void requireSchedulerToken(String token) {
    if (schedulerToken.isEmpty()
        || schedulerToken.get().isBlank()
        || token == null
        || !schedulerToken.get().equals(token)) {
      throw new ApiException(Status.UNAUTHORIZED, "unauthorized", "Unauthorized");
    }
  }
}
