package com.stocktracker.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic FX provider for dev + all tests. Returns fixed seeded daily rates (incl. {@code
 * USD<->SGD}) from {@code provider-stub/fx-rates.json}, so multi-currency conversion is
 * reproducible offline (SC-013).
 */
@ApplicationScoped
@Identifier("stub")
public class StubFxRateProvider implements FxRateProvider {
  @Inject ObjectMapper objectMapper;

  private volatile Map<String, BigDecimal> rates;

  @Override
  public List<ProviderFxRate> dailyRates(String base, Collection<String> quotes, LocalDate onDate) {
    var result = new ArrayList<ProviderFxRate>();
    for (var quote : quotes) {
      var rate = rate(base, quote);
      if (rate != null) {
        result.add(new ProviderFxRate(base.toUpperCase(), quote.toUpperCase(), onDate, rate));
      }
    }
    return result;
  }

  @Override
  public List<ProviderFxRate> rangeRates(
      String base, Collection<String> quotes, LocalDate from, LocalDate to) {
    if (from == null || to == null || to.isBefore(from)) {
      return List.of();
    }
    var result = new ArrayList<ProviderFxRate>();
    for (var date = from; !date.isAfter(to); date = date.plusDays(1)) {
      result.addAll(dailyRates(base, quotes, date));
    }
    return result;
  }

  private BigDecimal rate(String base, String quote) {
    if (base.equalsIgnoreCase(quote)) {
      return BigDecimal.ONE;
    }
    return rates().get(key(base, quote));
  }

  private static String key(String base, String quote) {
    return base.toUpperCase() + ":" + quote.toUpperCase();
  }

  private Map<String, BigDecimal> rates() {
    var local = rates;
    if (local == null) {
      synchronized (this) {
        if (rates == null) {
          rates = loadRates();
        }
        local = rates;
      }
    }
    return local;
  }

  private Map<String, BigDecimal> loadRates() {
    var map = new LinkedHashMap<String, BigDecimal>();
    try (InputStream stream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("provider-stub/fx-rates.json")) {
      if (stream == null) {
        return map;
      }
      JsonNode root = objectMapper.readTree(stream);
      for (JsonNode node : root.get("rates")) {
        map.put(
            key(node.get("base").asText(), node.get("quote").asText()),
            new BigDecimal(node.get("rate").asText()));
      }
    } catch (java.io.IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return map;
  }
}
