package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "watchlist_item")
public class WatchlistItem extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "watchlist_id", nullable = false)
  public UUID watchlistId;

  @Column(name = "instrument_symbol", nullable = false)
  public String instrumentSymbol;

  @Column(name = "display_order", nullable = false)
  public Integer displayOrder;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = LocalDateTime.now();
  }
}
