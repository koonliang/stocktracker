package com.stocktracker.api;

import com.stocktracker.dto.BaseCurrencyRequest;
import com.stocktracker.dto.BaseCurrencyResponse;
import com.stocktracker.service.SettingsService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Per-user base reporting currency. */
@Path("/api/me/base-currency")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class SettingsResource {
  @Inject SettingsService settingsService;

  @GET
  public BaseCurrencyResponse getBaseCurrency() {
    return settingsService.getBaseCurrency();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public BaseCurrencyResponse updateBaseCurrency(@Valid BaseCurrencyRequest request) {
    return settingsService.updateBaseCurrency(request.baseCurrency());
  }
}
