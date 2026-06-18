package com.stocktracker.service;

import com.stocktracker.domain.FxRate;
import com.stocktracker.persistence.FxRateRepository;
import com.stocktracker.service.provider.FxRateProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Backfills daily FX rows on demand for the exact currencies/date range needed by performance.
 * Existing rows are preserved; only missing pair/date combinations are fetched and inserted.
 */
@ApplicationScoped
public class FxHistoricalBackfillService {
  @Inject FxRateProvider fxRateProvider;
  @Inject FxRateRepository fxRates;

  @Inject FxHistoricalBackfillService self;

  public int backfillForBase(String baseCurrency, Set<String> quoteCurrencies, LocalDate from, LocalDate to) {
    if (baseCurrency == null || quoteCurrencies == null || quoteCurrencies.isEmpty()) {
      return 0;
    }
    if (from == null || to == null || to.isBefore(from)) {
      return 0;
    }
    var base = baseCurrency.toUpperCase();
    var quotes = new TreeSet<String>();
    for (var currency : quoteCurrencies) {
      if (currency == null || currency.isBlank()) {
        continue;
      }
      var normalized = currency.toUpperCase();
      if (!normalized.equals(base)) {
        quotes.add(normalized);
      }
    }
    if (quotes.isEmpty()) {
      return 0;
    }

    return self.persistMissingRates(fxRateProvider.rangeRates(base, quotes, from, to));
  }

  @Transactional(TxType.REQUIRES_NEW)
  int persistMissingRates(List<FxRateProvider.ProviderFxRate> rates) {
    var inserted = 0;
    for (var rate : rates) {
      if (rate.rate() == null) {
        continue;
      }
      inserted +=
          fxRates.insertIgnore(
              rate.base(), rate.quote(), rate.date(), rate.rate(), "fx-provider", false);
    }
    return inserted;
  }
}
