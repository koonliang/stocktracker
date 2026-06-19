package com.stocktracker.service.provider;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Production FX provider calling Frankfurter (ECB daily rates). Errors are caught and yield an
 * empty result so {@code FxRefreshJob} retains the last known rate (FX-unavailable edge case).
 */
@ApplicationScoped
@Identifier("frankfurter")
public class FrankfurterFxRateProvider implements FxRateProvider {
  private static final Logger LOG = Logger.getLogger(FrankfurterFxRateProvider.class);

  @Inject @RestClient FrankfurterApi api;

  @Override
  public List<ProviderFxRate> dailyRates(String base, Collection<String> quotes, LocalDate onDate) {
    var wanted = quotes.stream().filter(q -> !q.equalsIgnoreCase(base)).toList();
    if (wanted.isEmpty()) {
      return List.of();
    }
    try {
      var symbols = String.join(",", wanted);
      var response =
          onDate.equals(LocalDate.now())
              ? api.latest(base, symbols)
              : api.onDate(onDate.toString(), base, symbols);
      var rates = response.path("rates");
      var date = LocalDate.parse(response.path("date").asText(onDate.toString()));
      var result = new ArrayList<ProviderFxRate>();
      var fields = rates.fieldNames();
      while (fields.hasNext()) {
        var quote = fields.next();
        result.add(
            new ProviderFxRate(
                base.toUpperCase(),
                quote.toUpperCase(),
                date,
                new BigDecimal(rates.get(quote).asText())));
      }
      return result;
    } catch (RuntimeException exception) {
      LOG.warnf("Frankfurter dailyRates failed for base %s: %s", base, exception.getMessage());
      return List.of();
    }
  }

  @Override
  public List<ProviderFxRate> rangeRates(
      String base, Collection<String> quotes, LocalDate from, LocalDate to) {
    var wanted = quotes.stream().filter(q -> !q.equalsIgnoreCase(base)).toList();
    if (wanted.isEmpty() || from == null || to == null || to.isBefore(from)) {
      return List.of();
    }
    try {
      var parsed =
          parseRangeResponse(
              base,
              api.rangeV1(from.toString(), to.toString(), base, String.join(",", wanted)),
              from,
              to);
      return parsed.isEmpty() ? fallbackDailyLoop(base, wanted, from, to) : parsed;
    } catch (RuntimeException exception) {
      LOG.warnf(
          "Frankfurter rangeRates failed for base %s from %s to %s: %s",
          base, from, to, exception.getMessage());
      return fallbackDailyLoop(base, wanted, from, to);
    }
  }

  private List<ProviderFxRate> parseRangeResponse(
      String base, com.fasterxml.jackson.databind.JsonNode response, LocalDate from, LocalDate to) {
    var result = new ArrayList<ProviderFxRate>();
    var rates = response.path("rates");
    if (rates.isObject()) {
      var fields = rates.fields();
      while (fields.hasNext()) {
        Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
        var rowDate = parseDate(entry.getKey(), from);
        if (rowDate == null || rowDate.isBefore(from) || rowDate.isAfter(to)) {
          continue;
        }
        appendRates(result, base, rowDate, entry.getValue());
      }
      if (!result.isEmpty()) {
        return result;
      }
    }
    var data = response.path("data");
    if (data.isArray()) {
      for (var row : data) {
        var rowDate = parseDate(row.path("date").asText(null), from);
        if (rowDate == null || rowDate.isBefore(from) || rowDate.isAfter(to)) {
          continue;
        }
        appendRates(result, base, rowDate, row.path("rates"));
      }
    }
    return result;
  }

  private void appendRates(
      List<ProviderFxRate> out,
      String base,
      LocalDate date,
      com.fasterxml.jackson.databind.JsonNode ratesNode) {
    if (!ratesNode.isObject()) {
      return;
    }
    var fields = ratesNode.fields();
    while (fields.hasNext()) {
      var field = fields.next();
      var valueNode = field.getValue();
      if (valueNode == null || !valueNode.isNumber()) {
        continue;
      }
      out.add(
          new ProviderFxRate(
              base.toUpperCase(),
              field.getKey().toUpperCase(),
              date,
              new BigDecimal(valueNode.asText())));
    }
  }

  private LocalDate parseDate(String raw, LocalDate fallback) {
    try {
      return raw == null || raw.isBlank() ? fallback : LocalDate.parse(raw);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private List<ProviderFxRate> fallbackDailyLoop(
      String base, List<String> quotes, LocalDate from, LocalDate to) {
    var result = new ArrayList<ProviderFxRate>();
    for (var date = from; !date.isAfter(to); date = date.plusDays(1)) {
      result.addAll(dailyRates(base, quotes, date));
    }
    return result;
  }
}
