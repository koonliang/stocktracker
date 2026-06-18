package com.stocktracker.service;

import com.stocktracker.domain.FxRate;
import com.stocktracker.dto.ConversionDtos.FxStatus;
import com.stocktracker.persistence.FxRateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Converts amounts between currencies using the cached {@code fx_rate} table. Uses the exact date
 * when available, latest-prior rates as stale fallback, and marks truly missing pairs unavailable.
 */
@ApplicationScoped
public class CurrencyService {
  private static final String PIVOT = "USD";

  @Inject FxRateRepository fxRates;

  /** A converted amount and the FX metadata used to produce it. */
  public record Converted(BigDecimal value, LocalDate fxDate, FxStatus fxStatus) {
    public boolean stale() {
      return fxStatus == FxStatus.stale;
    }

    public boolean unavailable() {
      return fxStatus == FxStatus.unavailable;
    }
  }

  public Converted convert(BigDecimal amount, String from, String to, LocalDate onDate) {
    if (amount == null) {
      return new Converted(BigDecimal.ZERO, onDate, FxStatus.current);
    }
    if (from == null || to == null || from.equalsIgnoreCase(to)) {
      return new Converted(amount, onDate, FxStatus.current);
    }
    var rate = rate(from, to, onDate);
    if (rate.isEmpty()) {
      return new Converted(BigDecimal.ZERO, null, FxStatus.unavailable);
    }
    var value = amount.multiply(rate.get().value()).setScale(4, RoundingMode.HALF_UP);
    return new Converted(value, rate.get().fxDate(), rate.get().fxStatus());
  }

  public Converted convertTransaction(BigDecimal amount, String from, String to, LocalDate date) {
    return convert(amount, from, to, date);
  }

  public Converted convertHolding(BigDecimal amount, String from, String to, LocalDate valuationDate) {
    return convert(amount, from, to, valuationDate);
  }

  /** Resolved conversion rate from one currency to another, with a staleness flag. */
  public Optional<Converted> rate(String from, String to, LocalDate onDate) {
    if (from.equalsIgnoreCase(to)) {
      return Optional.of(new Converted(BigDecimal.ONE, onDate, FxStatus.current));
    }
    var direct = lookup(from, to, onDate);
    if (direct.isPresent()) {
      return direct.map(r -> new Converted(r.rate, r.rateDate, status(r, onDate)));
    }
    var inverse = lookup(to, from, onDate);
    if (inverse.isPresent()) {
      var r = inverse.get();
      var inverted = BigDecimal.ONE.divide(r.rate, 8, RoundingMode.HALF_UP);
      return Optional.of(new Converted(inverted, r.rateDate, status(r, onDate)));
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
        var fxDate = older(fromPivot.get().fxDate(), pivotTo.get().fxDate());
        var fxStatus =
            fromPivot.get().fxStatus() == FxStatus.current
                    && pivotTo.get().fxStatus() == FxStatus.current
                ? FxStatus.current
                : FxStatus.stale;
        return Optional.of(new Converted(crossed, fxDate, fxStatus));
      }
    }
    return Optional.empty();
  }

  private Optional<FxRate> lookup(String from, String to, LocalDate onDate) {
    var exact = fxRates.find(from, to, onDate);
    return exact.isPresent() ? exact : fxRates.findLatestOnOrBefore(from, to, onDate);
  }

  private FxStatus status(FxRate rate, LocalDate onDate) {
    if (rate.stale) {
      return FxStatus.stale;
    }
    return ChronoUnit.DAYS.between(rate.rateDate, onDate) > 1 ? FxStatus.stale : FxStatus.current;
  }

  private LocalDate older(LocalDate left, LocalDate right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return left.isBefore(right) ? left : right;
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
