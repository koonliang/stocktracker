package com.stocktracker.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.persistence.InstrumentRepository;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, network-free market-data provider for dev + all tests. Sources base prices from
 * seeded {@code instrument_price_bar} and from a small fixture ({@code provider-stub/quotes.json})
 * that adds at least one non-US (SGX {@code .SI}) symbol so global + multi-currency paths are
 * covered offline. Intra-day movement is a bounded function of symbol + the injectable clock, so
 * quotes "move" reproducibly across a poll without any network call.
 */
@ApplicationScoped
@Identifier("stub")
public class StubMarketDataProvider implements MarketDataProvider {
  @Inject InstrumentRepository instruments;
  @Inject ObjectMapper objectMapper;
  @Inject Clock clock;

  private volatile Map<String, StubSymbol> fixtures;

  record StubSymbol(
      String symbol,
      String name,
      String exchange,
      String currency,
      BigDecimal previousClose,
      BigDecimal price) {}

  @Override
  public List<ProviderQuote> latestQuotes(Collection<String> symbols) {
    var asOf = clock.instant();
    var quotes = new ArrayList<ProviderQuote>();
    for (var symbol : symbols) {
      var base = basePrices(symbol);
      if (base == null) {
        continue; // unknown to the provider — omitted (caller falls back to stale)
      }
      var price = move(symbol, base.current(), asOf);
      quotes.add(new ProviderQuote(symbol, price, base.previousClose(), asOf));
    }
    return quotes;
  }

  @Override
  public List<ProviderDailyBar> dailyHistory(String symbol, LocalDate from) {
    var fixture = fixtures().get(symbol.toUpperCase());
    if (fixture != null) {
      // Synthesize a short flat series from the fixture close so backfill has data offline.
      var bars = new ArrayList<ProviderDailyBar>();
      var start =
          from.isBefore(LocalDate.now(clock).minusDays(30))
              ? LocalDate.now(clock).minusDays(30)
              : from;
      for (var date = start; !date.isAfter(LocalDate.now(clock)); date = date.plusDays(1)) {
        bars.add(new ProviderDailyBar(symbol, date, fixture.price()));
      }
      return bars;
    }
    return instruments.listPriceBars(symbol).stream()
        .filter(bar -> !bar.tradeDate.isBefore(from))
        .map(bar -> new ProviderDailyBar(symbol, bar.tradeDate, bar.closePrice))
        .toList();
  }

  @Override
  public List<ProviderSymbol> searchSymbols(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    var results = new ArrayList<ProviderSymbol>();
    var lower = query.trim().toLowerCase();
    for (var instrument : instruments.search(query, 20)) {
      results.add(
          new ProviderSymbol(
              instrument.symbol, instrument.name, instrument.exchange, instrument.currency));
    }
    for (var fixture : fixtures().values()) {
      if (fixture.symbol().toLowerCase().contains(lower)
          || fixture.name().toLowerCase().contains(lower)) {
        results.add(
            new ProviderSymbol(
                fixture.symbol(), fixture.name(), fixture.exchange(), fixture.currency()));
      }
    }
    return results;
  }

  private record BasePrices(BigDecimal current, BigDecimal previousClose) {}

  private BasePrices basePrices(String symbol) {
    var fixture = fixtures().get(symbol.toUpperCase());
    if (fixture != null) {
      return new BasePrices(fixture.price(), fixture.previousClose());
    }
    var bars = instruments.listPriceBars(symbol);
    if (bars.isEmpty()) {
      return null;
    }
    var current = bars.get(bars.size() - 1).closePrice;
    var previous = bars.size() > 1 ? bars.get(bars.size() - 2).closePrice : current;
    return new BasePrices(current, previous);
  }

  /** Bounded deterministic intra-day movement in [-1%, +1%], changing each minute. */
  private BigDecimal move(String symbol, BigDecimal base, Instant asOf) {
    long minute = asOf.getEpochSecond() / 60;
    int hash = Math.abs((symbol + ":" + minute).hashCode());
    double delta = ((hash % 201) - 100) / 10000.0;
    return base.multiply(BigDecimal.valueOf(1 + delta)).setScale(4, RoundingMode.HALF_UP);
  }

  private Map<String, StubSymbol> fixtures() {
    var local = fixtures;
    if (local == null) {
      synchronized (this) {
        if (fixtures == null) {
          fixtures = loadFixtures();
        }
        local = fixtures;
      }
    }
    return local;
  }

  private Map<String, StubSymbol> loadFixtures() {
    var map = new LinkedHashMap<String, StubSymbol>();
    try (InputStream stream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("provider-stub/quotes.json")) {
      if (stream == null) {
        return map;
      }
      JsonNode root = objectMapper.readTree(stream);
      for (JsonNode node : root.get("symbols")) {
        var symbol = node.get("symbol").asText().toUpperCase();
        map.put(
            symbol,
            new StubSymbol(
                symbol,
                node.get("name").asText(),
                node.get("exchange").asText(),
                node.get("currency").asText(),
                new BigDecimal(node.get("previousClose").asText()),
                new BigDecimal(node.get("price").asText())));
      }
    } catch (java.io.IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return map;
  }
}
