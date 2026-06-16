package com.stocktracker.service.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Internal seam for foreign-exchange rates (contracts/fx-rate-provider.md), decoupled from the
 * quote provider. Selected at runtime by {@code stocktracker.fx.provider}: {@code stub} (default)
 * or {@code frankfurter} (prod). Daily granularity is sufficient (FR-030).
 */
public interface FxRateProvider {
  /** Daily rates for each quote currency against {@code base}, for {@code onDate}. */
  List<ProviderFxRate> dailyRates(String base, Collection<String> quotes, LocalDate onDate);

  /** Units of quote currency per 1 unit of base. */
  record ProviderFxRate(String base, String quote, LocalDate date, BigDecimal rate) {}
}
