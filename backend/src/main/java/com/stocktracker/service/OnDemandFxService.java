package com.stocktracker.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.Set;

/**
 * Ensures the exact FX pair/date needed by a transaction can be resolved before validation fails.
 * Reuses the historical-backfill path so old-dated transactions can warm the cache on demand.
 */
@ApplicationScoped
public class OnDemandFxService {
  @Inject CurrencyService currencyService;
  @Inject FxHistoricalBackfillService fxHistoricalBackfillService;

  public boolean ensureRate(String fromCurrency, String baseCurrency, LocalDate date) {
    if (fromCurrency == null || baseCurrency == null || date == null) {
      return false;
    }
    if (fromCurrency.equalsIgnoreCase(baseCurrency)) {
      return true;
    }
    if (currencyService.rate(fromCurrency, baseCurrency, date).isPresent()) {
      return true;
    }
    fxHistoricalBackfillService.backfillForBase(baseCurrency, Set.of(fromCurrency), date, date);
    return currencyService.rate(fromCurrency, baseCurrency, date).isPresent();
  }
}
