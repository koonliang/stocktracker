package com.stocktracker.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Production market-data provider calling Yahoo Finance's public endpoints. Endpoints are
 * unofficial; every call is wrapped so a transient/rate-limit error is logged and yields an empty
 * result rather than throwing into a request path (FR-006) — the caller serves the last cached
 * value and retries next cycle.
 */
@ApplicationScoped
@Identifier("yahoo")
public class YahooMarketDataProvider implements MarketDataProvider {
  private static final Logger LOG = Logger.getLogger(YahooMarketDataProvider.class);
  private static final List<String> SUPPORTED_SEARCH_QUOTE_TYPES = List.of("EQUITY", "ETF");
  private static final java.util.Map<String, String> YAHOO_SYMBOL_OVERRIDES =
      java.util.Map.of(
          "BRK.B", "BRK-B",
          "BF.B", "BF-B");

  @Inject @RestClient YahooApi api;

  @Override
  public List<ProviderQuote> latestQuotes(Collection<String> symbols) {
    if (symbols.isEmpty()) {
      return List.of();
    }
    // Yahoo's /v7/finance/quote now requires a session cookie + crumb (401 without it). The
    // /v8/finance/chart endpoint stays open and its `meta` block carries the current price and
    // previous close, so we read the quote from there — one call per symbol (FR-006: a failed
    // symbol just yields no quote and the cache keeps its prior value).
    var quotes = new ArrayList<ProviderQuote>();
    for (var symbol : symbols) {
      var quote = chartQuote(symbol);
      if (quote != null) {
        quotes.add(quote);
      }
    }
    return quotes;
  }

  private ProviderQuote chartQuote(String symbol) {
    try {
      var meta =
          api.chart(yahooSymbol(symbol), "1d", "1d").path("chart").path("result").path(0).path("meta");
      var price = decimal(meta, "regularMarketPrice");
      if (price == null) {
        return null;
      }
      var previousClose = decimal(meta, "chartPreviousClose");
      var asOf =
          meta.has("regularMarketTime")
              ? Instant.ofEpochSecond(meta.get("regularMarketTime").asLong())
              : Instant.now();
      return new ProviderQuote(symbol, price, previousClose, asOf);
    } catch (RuntimeException exception) {
      LOG.warnf("Yahoo latestQuotes failed for %s: %s", symbol, exception.getMessage());
      return null;
    }
  }

  @Override
  public List<ProviderDailyBar> dailyHistory(String symbol, LocalDate from) {
    return chartHistory(symbol, from, LocalDate.now(java.time.ZoneOffset.UTC));
  }

  @Override
  public List<ProviderDailyBar> dailyHistoryMax(String symbol) {
    var floor = LocalDate.of(1970, 1, 1);
    var today = LocalDate.now(java.time.ZoneOffset.UTC);
    var stitched = new TreeMap<LocalDate, ProviderDailyBar>();
    var windowEnd = today;
    var sawHistory = false;
    while (!windowEnd.isBefore(floor)) {
      var windowStart = max(windowEnd.minusYears(5).plusDays(1), floor);
      var bars = chartHistory(symbol, windowStart, windowEnd);
      if (bars.isEmpty()) {
        if (sawHistory) {
          break;
        }
      } else {
        sawHistory = true;
        for (var bar : bars) {
          stitched.put(bar.date(), bar);
        }
      }
      if (windowStart.equals(floor)) {
        break;
      }
      windowEnd = windowStart.minusDays(1);
    }
    return new ArrayList<>(stitched.values());
  }

  @Override
  public ProviderSnapshot latestSnapshot(String symbol) {
    try {
      var meta =
          api.chart(yahooSymbol(symbol), "1d", "1d").path("chart").path("result").path(0).path("meta");
      if (meta.isMissingNode()) {
        return null;
      }
      var asOf =
          meta.has("regularMarketTime")
              ? Instant.ofEpochSecond(meta.get("regularMarketTime").asLong())
                  .atZone(java.time.ZoneOffset.UTC)
                  .toLocalDate()
              : LocalDate.now(java.time.ZoneOffset.UTC);
      return new ProviderSnapshot(
          symbol.toUpperCase(),
          decimal(meta, "regularMarketOpen"),
          decimal(meta, "regularMarketDayHigh"),
          decimal(meta, "regularMarketDayLow"),
          decimal(meta, "chartPreviousClose"),
          longValue(meta, "regularMarketVolume"),
          decimal(meta, "fiftyTwoWeekHigh"),
          decimal(meta, "fiftyTwoWeekLow"),
          longValue(meta, "marketCap"),
          decimal(meta, "trailingPE"),
          asOf);
    } catch (RuntimeException exception) {
      LOG.warnf("Yahoo latestSnapshot failed for %s: %s", symbol, exception.getMessage());
      return null;
    }
  }

  private List<ProviderDailyBar> chartHistory(String symbol, LocalDate from, LocalDate to) {
    try {
      var response =
          api.chartPeriod(
              yahooSymbol(symbol),
              "1d",
              from.atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond(),
              to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond());
      var result = response.path("chart").path("result");
      if (!result.isArray() || result.isEmpty()) {
        return List.of();
      }
      var first = result.get(0);
      var timestamps = first.path("timestamp");
      var closes = first.path("indicators").path("quote").path(0).path("close");
      var bars = new ArrayList<ProviderDailyBar>();
      for (int i = 0; i < timestamps.size(); i++) {
        if (closes.get(i) == null || closes.get(i).isNull()) {
          continue;
        }
        var date =
            Instant.ofEpochSecond(timestamps.get(i).asLong())
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate();
        if (date.isBefore(from) || date.isAfter(to)) {
          continue;
        }
        bars.add(new ProviderDailyBar(symbol, date, new BigDecimal(closes.get(i).asText())));
      }
      return bars;
    } catch (WebApplicationException exception) {
      var response = exception.getResponse();
      var status = response == null ? -1 : response.getStatus();
      if (status == 400) {
        LOG.debugf("Yahoo dailyHistory boundary for %s %s..%s", symbol, from, to);
        return List.of();
      }
      LOG.warnf(
          "Yahoo dailyHistory failed for %s %s..%s: %s", symbol, from, to, exception.getMessage());
      return List.of();
    } catch (RuntimeException exception) {
      LOG.warnf(
          "Yahoo dailyHistory failed for %s %s..%s: %s", symbol, from, to, exception.getMessage());
      return List.of();
    }
  }

  private static LocalDate min(LocalDate left, LocalDate right) {
    return left.isBefore(right) ? left : right;
  }

  private static LocalDate max(LocalDate left, LocalDate right) {
    return left.isAfter(right) ? left : right;
  }

  @Override
  public List<ProviderSymbol> searchSymbols(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    try {
      var matches = api.search(query).path("quotes");
      var results = new ArrayList<ProviderSymbol>();
      for (JsonNode node : matches) {
        var symbol = node.path("symbol").asText(null);
        if (symbol == null
            || !SUPPORTED_SEARCH_QUOTE_TYPES.contains(node.path("quoteType").asText(""))) {
          continue;
        }
        results.add(
            new ProviderSymbol(
                symbol,
                node.path("longname").asText(node.path("shortname").asText(symbol)),
                node.path("exchDisp").asText(node.path("exchange").asText("")),
                // Search omits currency; the chart meta supplies it (quote endpoint now needs
                // auth).
                chartCurrency(symbol)));
      }
      return results;
    } catch (RuntimeException exception) {
      LOG.warnf("Yahoo searchSymbols failed for %s: %s", query, exception.getMessage());
      return List.of();
    }
  }

  private String chartCurrency(String symbol) {
    try {
      var meta =
          api.chart(yahooSymbol(symbol), "1d", "1d").path("chart").path("result").path(0).path("meta");
      return meta.path("currency").asText("USD");
    } catch (RuntimeException exception) {
      return "USD";
    }
  }

  static String yahooSymbol(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      return symbol;
    }
    var normalized = symbol.trim().toUpperCase();
    return YAHOO_SYMBOL_OVERRIDES.getOrDefault(normalized, normalized);
  }

  private static BigDecimal decimal(JsonNode node, String field) {
    var value = node.path(field);
    return value.isNumber() ? new BigDecimal(value.asText()) : null;
  }

  private static Long longValue(JsonNode node, String field) {
    var value = node.path(field);
    return value.isNumber() ? value.asLong() : null;
  }
}
