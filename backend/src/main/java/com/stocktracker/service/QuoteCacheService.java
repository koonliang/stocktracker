package com.stocktracker.service;

import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.dto.QuoteResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.service.provider.MarketDataProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Owns the {@code instrument_quote} cache: the scheduled job refreshes it from the provider, and
 * read endpoints serve from it (never calling the provider inline — FR-002). Staleness is derived
 * from {@code fetched_at} age (provider failing), not market hours. A stale/missing read triggers
 * an on-demand fetch to mitigate a cold scheduler (FR-006, plan "nuance").
 */
@ApplicationScoped
public class QuoteCacheService {
  @Inject MarketDataProvider marketDataProvider;
  @Inject QuoteRepository quoteRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject Clock clock;
  @Inject AlertEvaluationService alertEvaluationService;

  @ConfigProperty(name = "stocktracker.marketdata.refresh-interval", defaultValue = "60s")
  Duration refreshInterval;

  @ConfigProperty(name = "stocktracker.marketdata.stale-after-intervals", defaultValue = "3")
  int staleAfterIntervals;

  @ConfigProperty(name = "stocktracker.marketdata.provider", defaultValue = "stub")
  String providerId;

  /** Fetch fresh quotes for the symbols and upsert the cache. Never throws on provider failure. */
  @Transactional
  public void refreshSymbols(Collection<String> symbols) {
    var wanted = symbols.stream().map(String::toUpperCase).distinct().toList();
    if (wanted.isEmpty()) {
      return;
    }
    var fetched =
        marketDataProvider.latestQuotes(wanted).stream()
            .collect(Collectors.toMap(q -> q.symbol().toUpperCase(), q -> q, (a, b) -> a));
    var now = clock.instant();
    for (var symbol : wanted) {
      var quote = fetched.get(symbol);
      if (quote == null) {
        continue; // partial failure: keep the prior cached value, staleness recomputed on read
      }
      var row = quoteRepository.findOrNew(symbol);
      row.price = quote.price();
      row.previousClose = quote.previousClose();
      if (quote.price() != null && quote.previousClose() != null) {
        row.changeAmount = quote.price().subtract(quote.previousClose());
        row.changePct =
            quote.previousClose().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : row.changeAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(quote.previousClose(), 4, RoundingMode.HALF_UP);
      }
      row.asOf = quote.asOf();
      row.fetchedAt = now;
      row.source = providerId == null || providerId.isBlank() ? "stub" : providerId;
      row.stale = false;
      quoteRepository.persist(row); // fully populated before insert is scheduled
      alertEvaluationService.evaluate(row);
    }
  }

  /** Read cached quotes for the symbols, with stale fallback to the latest price bar (FR-006). */
  @Transactional
  public QuoteResponse readQuotes(Collection<String> symbols) {
    var wanted = symbols.stream().map(String::toUpperCase).distinct().toList();

    // On-demand refresh of stale/missing known instruments (mitigates a cold scheduler).
    var needsFetch =
        wanted.stream()
            .filter(instrumentRepository::existsSymbol)
            .filter(this::isStaleOrMissing)
            .toList();
    if (!needsFetch.isEmpty()) {
      refreshSymbols(needsFetch);
    }

    var quotes =
        quoteRepository.findBySymbols(wanted).stream()
            .collect(Collectors.toMap(q -> q.instrumentSymbol, q -> q, (a, b) -> a));
    var instruments = instrumentRepository.findBySymbols(wanted);

    var views = new ArrayList<QuoteResponse.QuoteView>();
    for (var symbol : wanted) {
      views.add(toView(symbol, quotes.get(symbol), instruments.get(symbol)));
    }
    return new QuoteResponse(views);
  }

  private boolean isStaleOrMissing(String symbol) {
    return quoteRepository.findBySymbol(symbol).map(this::effectiveStale).orElse(true);
  }

  /** Stale when explicitly flagged, never fetched, or the last fetch aged out past N intervals. */
  public boolean effectiveStale(InstrumentQuote quote) {
    if (quote.stale || quote.fetchedAt == null) {
      return true;
    }
    var maxAge = refreshInterval.multipliedBy(staleAfterIntervals);
    return Duration.between(quote.fetchedAt, clock.instant()).compareTo(maxAge) > 0;
  }

  private QuoteResponse.QuoteView toView(
      String symbol, InstrumentQuote quote, Instrument instrument) {
    var currency = instrument == null ? null : instrument.currency;
    if (quote != null && quote.price != null) {
      return new QuoteResponse.QuoteView(
          symbol,
          quote.price.doubleValue(),
          currency,
          dbl(quote.changeAmount),
          dbl(quote.changePct),
          dbl(quote.previousClose),
          quote.asOf,
          quote.fetchedAt,
          quote.source,
          effectiveStale(quote));
    }
    // No live quote: fall back to the latest price-bar close, marked stale.
    var bars =
        instrument == null
            ? List.<com.stocktracker.domain.InstrumentPriceBar>of()
            : instrumentRepository.listPriceBars(symbol);
    if (!bars.isEmpty()) {
      var close = bars.get(bars.size() - 1).closePrice;
      return new QuoteResponse.QuoteView(
          symbol, close.doubleValue(), currency, null, null, null, null, null, "price-bar", true);
    }
    // Unknown to the provider and no history: price null, stale true (FR-006).
    return new QuoteResponse.QuoteView(
        symbol, null, currency, null, null, null, null, null, null, true);
  }

  private static Double dbl(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  /** Map of symbol -> cached quote row, for the dashboard integration. */
  public Map<String, InstrumentQuote> cachedBySymbol(Collection<String> symbols) {
    var map = new HashMap<String, InstrumentQuote>();
    for (var quote : quoteRepository.findBySymbols(symbols)) {
      map.put(quote.instrumentSymbol, quote);
    }
    return map;
  }
}
