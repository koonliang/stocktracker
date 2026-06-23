package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.dto.AddInstrumentResponse;
import com.stocktracker.dto.InstrumentSearchResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.scheduler.FxRefreshJob;
import com.stocktracker.service.provider.MarketDataProvider;
import com.stocktracker.service.provider.ProviderConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
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
  @Inject ProviderConfig providerConfig;
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
    if (providerConfig.isLiveMarketDataProvider()) {
      historicalBackfillService.backfillTrailingYear(symbol);
    } else {
      historicalBackfillService.backfillMax(symbol);
    }
    fxRefreshJob.refresh(); // pick up a newly-introduced currency

    return buildResponse(instrument);
  }

  @Transactional
  public void refreshTrackedSymbols(Collection<String> symbols) {
    quoteCacheService.refreshSymbols(symbols);
  }

  @Transactional
  public void refreshTrackedSymbolsAndAnalysis(Collection<String> symbols) {
    var wanted = symbols.stream().map(String::toUpperCase).distinct().toList();
    if (wanted.isEmpty()) {
      return;
    }
    var today = LocalDate.now(clock);
    for (var symbol : wanted) {
      var bars = instrumentRepository.listPriceBars(symbol);
      if (providerConfig.isLiveMarketDataProvider()) {
        if (bars.isEmpty()) {
          historicalBackfillService.backfillTrailingYear(symbol);
        } else if (bars.get(0).tradeDate.isAfter(today.minusYears(5))) {
          historicalBackfillService.backfillMax(symbol);
        } else {
          historicalBackfillService.backfill(symbol, bars.get(bars.size() - 1).tradeDate);
        }
      } else {
        var from = bars.isEmpty() ? today.minusYears(5) : bars.get(bars.size() - 1).tradeDate;
        historicalBackfillService.backfill(symbol, from);
      }
      // QuoteRefreshJob owns instrument_quote updates; reusing the cached row here avoids
      // deadlocks when the history job overlaps the quote-refresh cadence.
      refreshSnapshotArtifacts(symbol, quoteRepository.findBySymbol(symbol).orElse(null));
    }
  }

  @Transactional
  public void bootstrapTrackedSymbolsAndAnalysis(Collection<String> symbols) {
    var wanted = symbols.stream().map(String::toUpperCase).distinct().toList();
    if (wanted.isEmpty()) {
      return;
    }
    quoteCacheService.refreshSymbols(wanted);
    for (var symbol : wanted) {
      if (instrumentRepository.listPriceBars(symbol).isEmpty()) {
        if (providerConfig.isLiveMarketDataProvider()) {
          historicalBackfillService.backfillTrailingYear(symbol);
        } else {
          historicalBackfillService.backfillMax(symbol);
        }
      }
      refreshSnapshotArtifacts(symbol, quoteRepository.findBySymbol(symbol).orElse(null));
    }
  }

  @Transactional
  public void rewriteTrackedSymbolsAndAnalysis(Collection<String> symbols) {
    var wanted = symbols.stream().map(String::toUpperCase).distinct().toList();
    if (wanted.isEmpty()) {
      return;
    }
    InstrumentPriceBar.delete("instrumentSymbol in ?1", wanted);
    InstrumentStat.delete("instrumentSymbol in ?1", wanted);
    quoteCacheService.refreshSymbols(wanted);
    for (var symbol : wanted) {
      historicalBackfillService.rewriteMax(symbol);
      refreshSnapshotArtifacts(symbol, quoteRepository.findBySymbol(symbol).orElse(null));
    }
  }

  private void refreshSnapshotArtifacts(String symbol, InstrumentQuote quote) {
    var snapshot = marketDataProvider.latestSnapshot(symbol);
    upsertPriceBar(symbol, quote, snapshot);
    upsertInstrumentStat(symbol, quote, snapshot);
  }

  private void upsertPriceBar(
      String symbol, InstrumentQuote quote, MarketDataProvider.ProviderSnapshot snapshot) {
    if (quote == null || quote.price == null) {
      return;
    }
    var tradeDate =
        snapshot != null && snapshot.asOfDate() != null
            ? snapshot.asOfDate()
            : quote.asOf == null
                ? LocalDate.now(clock)
                : quote.asOf.atZone(ZoneOffset.UTC).toLocalDate();
    var bar =
        instrumentRepository.findPriceBar(symbol, tradeDate).orElseGet(InstrumentPriceBar::new);
    bar.instrumentSymbol = symbol;
    bar.tradeDate = tradeDate;
    bar.openPrice =
        first(snapshot == null ? null : snapshot.openPrice(), quote.previousClose, quote.price);
    bar.highPrice = first(snapshot == null ? null : snapshot.highPrice(), quote.price);
    bar.lowPrice = first(snapshot == null ? null : snapshot.lowPrice(), quote.price);
    bar.closePrice = quote.price;
    bar.volume = snapshot != null && snapshot.volume() != null ? snapshot.volume() : 0L;
    if (bar.isPersistent()) {
      return;
    }
    bar.persist();
  }

  private void upsertInstrumentStat(
      String symbol, InstrumentQuote quote, MarketDataProvider.ProviderSnapshot snapshot) {
    var bars = instrumentRepository.listPriceBars(symbol);
    var latestBar = bars.isEmpty() ? null : bars.get(bars.size() - 1);
    if (snapshot == null && latestBar == null && quote == null) {
      return;
    }
    var stat = instrumentRepository.findStat(symbol).orElseGet(InstrumentStat::new);
    stat.instrumentSymbol = symbol;
    stat.openPrice =
        first(
            snapshot == null ? null : snapshot.openPrice(),
            latestBar == null ? null : latestBar.openPrice,
            quote == null ? null : quote.previousClose,
            quote == null ? null : quote.price);
    stat.highPrice =
        first(
            snapshot == null ? null : snapshot.highPrice(),
            latestBar == null ? null : latestBar.highPrice,
            quote == null ? null : quote.price);
    stat.lowPrice =
        first(
            snapshot == null ? null : snapshot.lowPrice(),
            latestBar == null ? null : latestBar.lowPrice,
            quote == null ? null : quote.price);
    stat.previousClose =
        first(
            snapshot == null ? null : snapshot.previousClose(),
            quote == null ? null : quote.previousClose,
            latestBar == null ? null : latestBar.closePrice,
            quote == null ? null : quote.price);
    stat.volume =
        snapshot != null && snapshot.volume() != null
            ? snapshot.volume()
            : latestBar == null ? 0L : latestBar.volume;
    stat.week52High =
        first(
            snapshot == null ? null : snapshot.week52High(),
            trailingHigh(bars),
            latestBar == null ? null : latestBar.highPrice,
            quote == null ? null : quote.price);
    stat.week52Low =
        first(
            snapshot == null ? null : snapshot.week52Low(),
            trailingLow(bars),
            latestBar == null ? null : latestBar.lowPrice,
            quote == null ? null : quote.price);
    stat.marketCap =
        snapshot != null && snapshot.marketCap() != null
            ? snapshot.marketCap()
            : stat.marketCap == null ? 0L : stat.marketCap;
    stat.peRatio =
        snapshot != null && snapshot.peRatio() != null ? snapshot.peRatio() : stat.peRatio;
    stat.asOfDate =
        snapshot != null && snapshot.asOfDate() != null
            ? snapshot.asOfDate()
            : latestBar == null ? LocalDate.now(clock) : latestBar.tradeDate;
    if (!stat.isPersistent()) {
      stat.persist();
    }
  }

  @SafeVarargs
  private static <T> T first(T... values) {
    for (var value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private java.math.BigDecimal trailingHigh(List<InstrumentPriceBar> bars) {
    if (bars.isEmpty()) {
      return null;
    }
    var cutoff = bars.get(bars.size() - 1).tradeDate.minusYears(1);
    return bars.stream()
        .filter(bar -> !bar.tradeDate.isBefore(cutoff))
        .map(bar -> bar.highPrice)
        .max(java.util.Comparator.naturalOrder())
        .orElse(null);
  }

  private java.math.BigDecimal trailingLow(List<InstrumentPriceBar> bars) {
    if (bars.isEmpty()) {
      return null;
    }
    var cutoff = bars.get(bars.size() - 1).tradeDate.minusYears(1);
    return bars.stream()
        .filter(bar -> !bar.tradeDate.isBefore(cutoff))
        .map(bar -> bar.lowPrice)
        .min(java.util.Comparator.naturalOrder())
        .orElse(null);
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
