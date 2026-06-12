package com.stocktracker.service;

import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.service.provider.MarketDataProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Backfills daily-close history into {@code instrument_price_bar} on demand — for newly-added
 * symbols (FR-027) and when performance analytics finds gaps (FR-025). The provider supplies only
 * the close, so open/high/low mirror it and volume is 0; existing dates are left untouched.
 */
@ApplicationScoped
public class HistoricalBackfillService {
  @Inject MarketDataProvider marketDataProvider;
  @Inject InstrumentRepository instrumentRepository;

  @Transactional
  public int backfill(String symbol, LocalDate from) {
    var existingDates =
        instrumentRepository.listPriceBars(symbol).stream()
            .map(bar -> bar.tradeDate)
            .collect(Collectors.toSet());
    var inserted = 0;
    for (var providerBar : marketDataProvider.dailyHistory(symbol, from)) {
      if (providerBar.close() == null || existingDates.contains(providerBar.date())) {
        continue;
      }
      var bar = new InstrumentPriceBar();
      bar.instrumentSymbol = symbol.toUpperCase();
      bar.tradeDate = providerBar.date();
      bar.openPrice = providerBar.close();
      bar.highPrice = providerBar.close();
      bar.lowPrice = providerBar.close();
      bar.closePrice = providerBar.close();
      bar.volume = 0L;
      bar.persist();
      existingDates.add(providerBar.date());
      inserted++;
    }
    return inserted;
  }
}
