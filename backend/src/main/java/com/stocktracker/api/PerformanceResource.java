package com.stocktracker.api;

import com.stocktracker.dto.PerformanceResponse;
import com.stocktracker.service.PerformanceService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/performance")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class PerformanceResource {
  @Inject PerformanceService performanceService;

  @GET
  public PerformanceResponse get(
      @QueryParam("window") String window, @QueryParam("method") String method) {
    return performanceService.performance(window, method);
  }
}
