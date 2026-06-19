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
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
public class Alert extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Column(name = "instrument_symbol", nullable = false)
  public String instrumentSymbol;

  @Column(name = "condition_type", nullable = false)
  public String conditionType;

  @Column(nullable = false, precision = 19, scale = 4)
  public BigDecimal threshold;

  @Column(nullable = false)
  public boolean armed = true;

  @Column(name = "last_condition_met")
  public Boolean lastConditionMet;

  @Column(name = "last_triggered_at")
  public Instant lastTriggeredAt;

  @Column(name = "last_cleared_at")
  public Instant lastClearedAt;

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
