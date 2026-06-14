package com.stocktracker.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Frankfurter (ECB daily reference rates, no API key). Base URL set by config key {@code
 * frankfurter}.
 */
@RegisterRestClient(configKey = "frankfurter")
@Produces(MediaType.APPLICATION_JSON)
public interface FrankfurterApi {
  @GET
  @Path("/latest")
  JsonNode latest(@QueryParam("base") String base, @QueryParam("symbols") String symbols);

  @GET
  @Path("/{date}")
  JsonNode onDate(
      @PathParam("date") String date,
      @QueryParam("base") String base,
      @QueryParam("symbols") String symbols);
}
