package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument_stat")
public class InstrumentStat extends PanacheEntityBase {
  @Id
  @Column(name = "instrument_symbol")
  public String instrumentSymbol;

  @Column(name = "open_price", nullable = false, precision = 19, scale = 4)
  public BigDecimal openPrice;

  @Column(name = "high_price", nullable = false, precision = 19, scale = 4)
  public BigDecimal highPrice;

  @Column(name = "low_price", nullable = false, precision = 19, scale = 4)
  public BigDecimal lowPrice;

  @Column(name = "previous_close", nullable = false, precision = 19, scale = 4)
  public BigDecimal previousClose;

  @Column(nullable = false)
  public Long volume;

  @Column(name = "week_52_high", nullable = false, precision = 19, scale = 4)
  public BigDecimal week52High;

  @Column(name = "week_52_low", nullable = false, precision = 19, scale = 4)
  public BigDecimal week52Low;

  @Column(name = "market_cap", nullable = false)
  public Long marketCap;

  @Column(name = "pe_ratio", precision = 19, scale = 4)
  public BigDecimal peRatio;

  @Column(name = "as_of_date", nullable = false)
  public LocalDate asOfDate;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
