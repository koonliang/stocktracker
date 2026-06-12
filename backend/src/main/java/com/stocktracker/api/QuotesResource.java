package com.stocktracker.api;

import com.stocktracker.dto.QuoteResponse;
import com.stocktracker.service.QuoteCacheService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

/** Reads the latest cached quotes for the requested symbols; never calls the provider inline. */
@Path("/api/quotes")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class QuotesResource {
  @Inject QuoteCacheService quoteCacheService;

  @GET
  public QuoteResponse getQuotes(@QueryParam("symbols") String symbols) {
    if (symbols == null || symbols.isBlank()) {
      return new QuoteResponse(List.of());
    }
    var list =
        Arrays.stream(symbols.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    return quoteCacheService.readQuotes(list);
  }
}
