package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument")
public class Instrument extends PanacheEntityBase {
  @Id public String symbol;

  @Column(nullable = false)
  public String name;

  @Column(nullable = false)
  public String sector;

  @Column(nullable = false)
  public String exchange;

  /** Native trading currency (FR-029); defaults to USD for seeded US instruments. */
  @Column(nullable = false, length = 3)
  public String currency = "USD";

  @Column(nullable = false)
  public boolean active;

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
