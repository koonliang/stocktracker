package com.stocktracker.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
      var meta = api.chart(symbol, "1d", "1d").path("chart").path("result").path(0).path("meta");
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
    try {
      var days = Math.max(1, ChronoUnit.DAYS.between(from, LocalDate.now()));
      var range = days <= 30 ? "1mo" : days <= 90 ? "3mo" : days <= 365 ? "1y" : "5y";
      var response = api.chart(symbol, "1d", range);
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
        if (date.isBefore(from)) {
          continue;
        }
        bars.add(new ProviderDailyBar(symbol, date, new BigDecimal(closes.get(i).asText())));
      }
      return bars;
    } catch (RuntimeException exception) {
      LOG.warnf("Yahoo dailyHistory failed for %s: %s", symbol, exception.getMessage());
      return List.of();
    }
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
                // Search omits currency; the chart meta supplies it (quote endpoint now needs auth).
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
      var meta = api.chart(symbol, "1d", "1d").path("chart").path("result").path(0).path("meta");
      return meta.path("currency").asText("USD");
    } catch (RuntimeException exception) {
      return "USD";
    }
  }

  private static BigDecimal decimal(JsonNode node, String field) {
    var value = node.path(field);
    return value.isNumber() ? new BigDecimal(value.asText()) : null;
  }
}
