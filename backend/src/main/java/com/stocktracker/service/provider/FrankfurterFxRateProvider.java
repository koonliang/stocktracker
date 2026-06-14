package com.stocktracker.service.provider;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
}
