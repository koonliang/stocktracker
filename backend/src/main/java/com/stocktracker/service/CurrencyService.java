package com.stocktracker.service;

import com.stocktracker.domain.FxRate;
import com.stocktracker.persistence.FxRateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Converts amounts between currencies using the cached {@code fx_rate} table (contracts/currency-
 * api.md). Uses the rate for the date (or the most recent prior date); on a missing direct pair it
 * inverts or cross-converts via a USD pivot. A converted value backed by no usable rate is returned
 * unchanged and flagged stale rather than failing the view (FX-unavailable edge case).
 */
@ApplicationScoped
public class CurrencyService {
  private static final String PIVOT = "USD";

  @Inject FxRateRepository fxRates;

  /** A converted amount and whether the rate behind it was a stale/last-known value. */
  public record Converted(BigDecimal value, boolean stale) {}

  public Converted convert(BigDecimal amount, String from, String to, LocalDate onDate) {
    if (amount == null) {
      return new Converted(BigDecimal.ZERO, false);
    }
    if (from == null || to == null || from.equalsIgnoreCase(to)) {
      return new Converted(amount, false);
    }
    var rate = rate(from, to, onDate);
    if (rate.isEmpty()) {
      return new Converted(amount, true); // no usable rate — pass through, flagged stale
    }
    var value = amount.multiply(rate.get().value()).setScale(4, RoundingMode.HALF_UP);
    return new Converted(value, rate.get().stale());
  }

  /** Resolved conversion rate from one currency to another, with a staleness flag. */
  public Optional<Converted> rate(String from, String to, LocalDate onDate) {
    if (from.equalsIgnoreCase(to)) {
      return Optional.of(new Converted(BigDecimal.ONE, false));
    }
    var direct = lookup(from, to, onDate);
    if (direct.isPresent()) {
      return direct.map(r -> new Converted(r.rate, stale(r, onDate)));
    }
    var inverse = lookup(to, from, onDate);
    if (inverse.isPresent()) {
      var r = inverse.get();
      var inverted = BigDecimal.ONE.divide(r.rate, 8, RoundingMode.HALF_UP);
      return Optional.of(new Converted(inverted, stale(r, onDate)));
    }
    // Cross-convert via the USD pivot: from -> USD -> to.
    if (!from.equalsIgnoreCase(PIVOT) && !to.equalsIgnoreCase(PIVOT)) {
      var fromPivot = rate(from, PIVOT, onDate);
      var pivotTo = rate(PIVOT, to, onDate);
      if (fromPivot.isPresent() && pivotTo.isPresent()) {
        var crossed =
            fromPivot
                .get()
                .value()
                .multiply(pivotTo.get().value())
                .setScale(8, RoundingMode.HALF_UP);
        return Optional.of(
            new Converted(crossed, fromPivot.get().stale() || pivotTo.get().stale()));
      }
    }
    return Optional.empty();
  }

  private Optional<FxRate> lookup(String from, String to, LocalDate onDate) {
    var exact = fxRates.find(from, to, onDate);
    return exact.isPresent() ? exact : fxRates.findLatestOnOrBefore(from, to, onDate);
  }

  private boolean stale(FxRate rate, LocalDate onDate) {
    return rate.stale || !rate.rateDate.isEqual(onDate);
  }

  /** Currencies the FX cache can convert between (for the base-currency picker). */
  public TreeSet<String> supportedCurrencies(String defaultBase) {
    var currencies = new TreeSet<String>();
    currencies.add(defaultBase.toUpperCase());
    for (var rate : FxRate.<FxRate>listAll()) {
      currencies.add(rate.baseCurrency);
      currencies.add(rate.quoteCurrency);
    }
    return currencies;
  }
}
