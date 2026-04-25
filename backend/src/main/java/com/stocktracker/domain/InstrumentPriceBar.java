package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument_price_bar")
public class InstrumentPriceBar extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "instrument_symbol", nullable = false)
  public String instrumentSymbol;

  @Column(name = "trade_date", nullable = false)
  public LocalDate tradeDate;

  @Column(name = "open_price", nullable = false, precision = 19, scale = 4)
  public BigDecimal openPrice;

  @Column(name = "high_price", nullable = false, precision = 19, scale = 4)
  public BigDecimal highPrice;

  @Column(name = "low_price", nullable = false, precision = 19, scale = 4)
  public BigDecimal lowPrice;

  @Column(name = "close_price", nullable = false, precision = 19, scale = 4)
  public BigDecimal closePrice;

  @Column(nullable = false)
  public Long volume;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
