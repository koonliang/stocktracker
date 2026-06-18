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
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
public class Notification extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Column(name = "alert_id")
  public Long alertId;

  @Column(nullable = false)
  public String message;

  @Column(name = "instrument_symbol")
  public String instrumentSymbol;

  @Column(name = "condition_type")
  public String conditionType;

  @Column(precision = 19, scale = 4)
  public BigDecimal threshold;

  @Column(name = "observed_value", precision = 19, scale = 4)
  public BigDecimal observedValue;

  @Column(name = "observed_currency", length = 3)
  public String observedCurrency;

  @Column(name = "triggered_at")
  public LocalDateTime triggeredAt;

  @Column(name = "crossing_key", length = 64)
  public String crossingKey;

  @Column(name = "is_read", nullable = false)
  public boolean read;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @Column(name = "updated_at")
  public LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    var now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
    if (triggeredAt == null) {
      triggeredAt = now;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
