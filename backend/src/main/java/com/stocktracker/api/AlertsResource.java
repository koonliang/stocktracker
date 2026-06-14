package com.stocktracker.api;

import com.stocktracker.dto.AlertDtos.AlertListResponse;
import com.stocktracker.dto.AlertDtos.AlertRequest;
import com.stocktracker.dto.AlertDtos.AlertView;
import com.stocktracker.service.AlertService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/alerts")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertsResource {
  @Inject AlertService alertService;

  @GET
  public AlertListResponse list() {
    return alertService.list();
  }

  @POST
  public Response create(AlertRequest request) {
    return Response.status(Response.Status.CREATED).entity(alertService.create(request)).build();
  }

  @PATCH
  @Path("/{id}")
  public AlertView update(@PathParam("id") Long id, AlertRequest request) {
    return alertService.update(id, request);
  }

  @DELETE
  @Path("/{id}")
  public void delete(@PathParam("id") Long id) {
    alertService.delete(id);
  }
}
