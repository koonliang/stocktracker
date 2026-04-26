package com.stocktracker.api;

import com.stocktracker.dto.DashboardResponse;
import com.stocktracker.service.PortfolioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {
  @Inject PortfolioService portfolioService;

  @GET
  public DashboardResponse getDashboard() {
    return portfolioService.getDashboard();
  }
}
