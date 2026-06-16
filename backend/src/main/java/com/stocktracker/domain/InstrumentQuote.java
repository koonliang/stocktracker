package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Latest live quote per symbol (one row, upserted by QuoteRefreshJob). Staleness is judged by
 * {@code fetchedAt} age — provider failing — not by whether the market is open (data-model.md).
 */
@Entity
@Table(name = "instrument_quote")
public class InstrumentQuote extends PanacheEntityBase {
  @Id
  @Column(name = "instrument_symbol")
  public String instrumentSymbol;

  @Column(precision = 19, scale = 4)
  public BigDecimal price;

  @Column(name = "change_amount", precision = 19, scale = 4)
  public BigDecimal changeAmount;

  @Column(name = "change_pct", precision = 9, scale = 4)
  public BigDecimal changePct;

  @Column(name = "previous_close", precision = 19, scale = 4)
  public BigDecimal previousClose;

  /** Provider's market timestamp for the value. */
  @Column(name = "as_of")
  public Instant asOf;

  /** When our job last successfully obtained a value (drives staleness). */
  @Column(name = "fetched_at")
  public Instant fetchedAt;

  @Column(nullable = false)
  public String source;

  @Column(nullable = false)
  public boolean stale;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = LocalDateTime.now();
  }
}
