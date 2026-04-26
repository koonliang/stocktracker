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
@Table(name = "watchlist_item")
public class WatchlistItem extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "watchlist_id", nullable = false)
  public Long watchlistId;

  @Column(name = "instrument_symbol", nullable = false)
  public String instrumentSymbol;

  @Column(name = "display_order", nullable = false)
  public Integer displayOrder;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
