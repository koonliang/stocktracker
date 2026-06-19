package com.stocktracker.persistence;

import com.stocktracker.domain.FxRate;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class FxRateRepository implements PanacheRepository<FxRate> {
  @Inject EntityManager entityManager;

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

  /** Insert the rate row if it does not already exist for the unique pair/date key. */
  public int insertIgnore(
      String base, String quote, LocalDate rateDate, java.math.BigDecimal rate, String source, boolean stale) {
    return entityManager
        .createNativeQuery(
            """
            INSERT IGNORE INTO fx_rate
              (base_currency, quote_currency, rate_date, rate, source, stale)
            VALUES (?1, ?2, ?3, ?4, ?5, ?6)
            """)
        .setParameter(1, base.toUpperCase())
        .setParameter(2, quote.toUpperCase())
        .setParameter(3, rateDate)
        .setParameter(4, rate)
        .setParameter(5, source)
        .setParameter(6, stale)
        .executeUpdate();
  }
}
