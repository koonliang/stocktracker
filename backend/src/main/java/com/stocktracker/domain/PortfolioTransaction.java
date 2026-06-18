package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_transaction")
public class PortfolioTransaction extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Column(name = "trade_date", nullable = false)
  public LocalDate tradeDate;

  /** Null for cash events (deposit/withdrawal/fee); required for security/dividend/split. */
  @Column(name = "instrument_symbol")
  public String instrumentSymbol;

  /** One of: buy, sell, dividend, split, deposit, withdrawal, fee. */
  @Column(name = "transaction_type", nullable = false)
  public String transactionType;

  @Column(nullable = false, precision = 19, scale = 6)
  public BigDecimal quantity;

  @Column(nullable = false, precision = 19, scale = 4)
  public BigDecimal price;

  @Column(nullable = false, precision = 19, scale = 4)
  public BigDecimal fees;

  /** Cash value for dividend/deposit/withdrawal/fee (FR-007). */
  @Column(precision = 19, scale = 4)
  public BigDecimal amount;

  /** Defaults to the instrument currency for security txns; required for cash txns. */
  @Column(length = 3)
  public String currency;

  /** How the currency was determined: instrument, manual, import, user_base_backfill. */
  @Column(name = "currency_source", length = 32)
  public String currencySource;

  /** When the currency was backfilled (legacy rows only). */
  @Column(name = "currency_backfilled_at")
  public LocalDateTime currencyBackfilledAt;

  @Column(nullable = false)
  public String source;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    var now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
