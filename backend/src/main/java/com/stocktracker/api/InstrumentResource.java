package com.stocktracker.api;

import com.stocktracker.dto.InstrumentAnalysisResponse;
import com.stocktracker.service.InstrumentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/instruments")
@Produces(MediaType.APPLICATION_JSON)
public class InstrumentResource {
  @Inject InstrumentService instrumentService;

  @GET
  @Path("/{ticker}")
  public InstrumentAnalysisResponse getInstrument(@PathParam("ticker") String ticker) {
    return instrumentService.getAnalysis(ticker);
  }
}
