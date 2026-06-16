package com.stocktracker.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Yahoo Finance public JSON endpoints (no API key). Base URL set by config key {@code yahoo}. */
@RegisterRestClient(configKey = "yahoo")
@Produces(MediaType.APPLICATION_JSON)
@ClientHeaderParam(name = "User-Agent", value = "Mozilla/5.0 (compatible; stocktracker/1.0)")
public interface YahooApi {
  @GET
  @Path("/v7/finance/quote")
  JsonNode quote(@QueryParam("symbols") String symbols);

  @GET
  @Path("/v8/finance/chart/{symbol}")
  JsonNode chart(
      @PathParam("symbol") String symbol,
      @QueryParam("interval") String interval,
      @QueryParam("range") String range);

  @GET
  @Path("/v1/finance/search")
  JsonNode search(@QueryParam("q") String query);
}
