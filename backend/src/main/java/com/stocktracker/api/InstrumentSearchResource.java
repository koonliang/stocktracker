package com.stocktracker.api;

import com.stocktracker.dto.AddInstrumentRequest;
import com.stocktracker.dto.AddInstrumentResponse;
import com.stocktracker.dto.InstrumentSearchResponse;
import com.stocktracker.service.MarketDataService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Symbol search + add-on-demand (instruments-search-api.md). */
@Path("/api/instruments")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class InstrumentSearchResource {
  @Inject MarketDataService marketDataService;

  @GET
  @Path("/search")
  public InstrumentSearchResponse search(@QueryParam("q") String query) {
    return marketDataService.search(query);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addInstrument(@Valid AddInstrumentRequest request) {
    AddInstrumentResponse response = marketDataService.addInstrument(request.symbol());
    return Response.status(Response.Status.CREATED).entity(response).build();
  }
}
