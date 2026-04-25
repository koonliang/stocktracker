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
import java.util.UUID;

@Entity
@Table(name = "portfolio_transaction")
public class PortfolioTransaction extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "trade_date", nullable = false)
  public LocalDate tradeDate;

  @Column(name = "instrument_symbol", nullable = false)
  public String instrumentSymbol;

  @Column(name = "transaction_type", nullable = false)
  public String transactionType;

  @Column(nullable = false, precision = 19, scale = 6)
  public BigDecimal quantity;

  @Column(nullable = false, precision = 19, scale = 4)
  public BigDecimal price;

  @Column(nullable = false, precision = 19, scale = 4)
  public BigDecimal fees;

  @Column(nullable = false)
  public String source;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    var now = LocalDateTime.now();
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
