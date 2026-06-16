package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.dto.BaseCurrencyResponse;
import com.stocktracker.scheduler.FxRefreshJob;
import com.stocktracker.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Reads/updates the current user's base reporting currency (contracts/currency-api.md). */
@ApplicationScoped
public class SettingsService {
  @Inject CurrentUser currentUser;
  @Inject CurrencyService currencyService;
  @Inject FxRefreshJob fxRefreshJob;

  @ConfigProperty(name = "stocktracker.base-currency.default", defaultValue = "USD")
  String defaultBaseCurrency;

  public BaseCurrencyResponse getBaseCurrency() {
    return new BaseCurrencyResponse(currentUser.require().baseCurrency, supported());
  }

  @Transactional
  public BaseCurrencyResponse updateBaseCurrency(String rawCurrency) {
    var currency = rawCurrency.trim().toUpperCase();
    if (!supported().contains(currency)) {
      throw new ApiException(
          ApiStatuses.UNPROCESSABLE_ENTITY,
          "unsupported_currency",
          "Unsupported base currency: " + currency);
    }
    var user = currentUser.require();
    user.baseCurrency = currency;
    fxRefreshJob.refresh(); // ensure rates exist for the newly-chosen base
    return new BaseCurrencyResponse(currency, supported());
  }

  private List<String> supported() {
    return List.copyOf(currencyService.supportedCurrencies(defaultBaseCurrency));
  }
}
