package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.domain.Instrument;
import com.stocktracker.dto.AddInstrumentResponse;
import com.stocktracker.dto.InstrumentSearchResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.scheduler.FxRefreshJob;
import com.stocktracker.service.provider.MarketDataProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Symbol search proxy + add-on-demand (instruments-search-api.md). Adding a symbol creates the
 * instrument from the provider's metadata, then immediately fetches a quote, backfills history, and
 * refreshes FX so price/value appear at once (FR-027/SC-010). Unrecognized symbols are rejected
 * with no row created (edge case).
 */
@ApplicationScoped
public class MarketDataService {
  @Inject MarketDataProvider marketDataProvider;
  @Inject InstrumentRepository instrumentRepository;
  @Inject QuoteRepository quoteRepository;
  @Inject QuoteCacheService quoteCacheService;
  @Inject HistoricalBackfillService historicalBackfillService;
  @Inject FxRefreshJob fxRefreshJob;
  @Inject Clock clock;

  public InstrumentSearchResponse search(String query) {
    if (query == null || query.isBlank()) {
      return new InstrumentSearchResponse(List.of());
    }
    var results =
        marketDataProvider.searchSymbols(query).stream()
            .map(
                s ->
                    new InstrumentSearchResponse.Result(
                        s.symbol(), s.name(), s.exchange(), s.currency()))
            .toList();
    return new InstrumentSearchResponse(results);
  }

  @Transactional
  public AddInstrumentResponse addInstrument(String rawSymbol) {
    var symbol = rawSymbol.trim().toUpperCase();
    var existing = instrumentRepository.findBySymbol(symbol).orElse(null);
    if (existing != null) {
      quoteCacheService.refreshSymbols(List.of(symbol)); // idempotent: ensure a fresh quote
      return buildResponse(existing);
    }

    var match =
        marketDataProvider.searchSymbols(symbol).stream()
            .filter(s -> s.symbol().equalsIgnoreCase(symbol))
            .findFirst()
            .orElseThrow(
                () ->
                    new ApiException(
                        ApiStatuses.UNPROCESSABLE_ENTITY,
                        "unknown_symbol",
                        "Symbol not recognized: " + symbol));

    var instrument = new Instrument();
    instrument.symbol = match.symbol().toUpperCase();
    instrument.name = match.name();
    instrument.sector = "Unknown";
    instrument.exchange = match.exchange() == null ? "" : match.exchange();
    instrument.currency = match.currency() == null ? "USD" : match.currency().toUpperCase();
    instrument.active = true;
    instrumentRepository.persist(instrument);

    // Immediate quote + history so price/value appear at once; tolerate provider failure (stale).
    quoteCacheService.refreshSymbols(List.of(symbol));
    historicalBackfillService.backfill(symbol, LocalDate.now(clock).minusYears(5));
    fxRefreshJob.refresh(); // pick up a newly-introduced currency

    return buildResponse(instrument);
  }

  private AddInstrumentResponse buildResponse(Instrument instrument) {
    var quote = quoteRepository.findBySymbol(instrument.symbol).orElse(null);
    var summary =
        quote == null
            ? new AddInstrumentResponse.QuoteSummary(null, null, true)
            : new AddInstrumentResponse.QuoteSummary(
                quote.price == null ? null : quote.price.doubleValue(),
                quote.asOf,
                quoteCacheService.effectiveStale(quote));
    return new AddInstrumentResponse(
        instrument.symbol, instrument.name, instrument.exchange, instrument.currency, summary);
  }
}
