package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A daily exchange rate: units of {@code quoteCurrency} per 1 unit of {@code baseCurrency}. */
@Entity
@Table(name = "fx_rate")
public class FxRate extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "base_currency", nullable = false, length = 3)
  public String baseCurrency;

  @Column(name = "quote_currency", nullable = false, length = 3)
  public String quoteCurrency;

  @Column(name = "rate_date", nullable = false)
  public LocalDate rateDate;

  @Column(nullable = false, precision = 19, scale = 8)
  public BigDecimal rate;

  @Column(nullable = false)
  public String source;

  @Column(nullable = false)
  public boolean stale;
}
