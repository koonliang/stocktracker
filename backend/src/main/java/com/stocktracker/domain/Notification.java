package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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

  @Column(name = "is_read", nullable = false)
  public boolean read;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
