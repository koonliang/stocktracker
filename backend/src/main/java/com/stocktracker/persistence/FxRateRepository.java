package com.stocktracker.persistence;

import com.stocktracker.domain.FxRate;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class FxRateRepository implements PanacheRepository<FxRate> {
  /** Rate for the exact pair + date, if present. */
  public Optional<FxRate> find(String base, String quote, LocalDate onDate) {
    return find(
            "baseCurrency = ?1 and quoteCurrency = ?2 and rateDate = ?3",
            base.toUpperCase(),
            quote.toUpperCase(),
            onDate)
        .firstResultOptional();
  }

  /** Most recent rate for the pair on or before the given date (last-known fallback). */
  public Optional<FxRate> findLatestOnOrBefore(String base, String quote, LocalDate onDate) {
    return find(
            "baseCurrency = ?1 and quoteCurrency = ?2 and rateDate <= ?3 order by rateDate desc",
            base.toUpperCase(),
            quote.toUpperCase(),
            onDate)
        .firstResultOptional();
  }

  /** The existing (managed) rate row for a pair + date, or a new transient one to be persisted. */
  public FxRate findOrNew(String base, String quote, LocalDate onDate) {
    return find(base, quote, onDate)
        .orElseGet(
            () -> {
              var row = new FxRate();
              row.baseCurrency = base.toUpperCase();
              row.quoteCurrency = quote.toUpperCase();
              row.rateDate = onDate;
              return row;
            });
  }
}
