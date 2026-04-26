package com.stocktracker.persistence;

import com.stocktracker.domain.Watchlist;
import com.stocktracker.domain.WatchlistItem;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WatchlistRepository implements PanacheRepositoryBase<Watchlist, Long> {
  public Optional<Watchlist> findByNameIgnoreCase(String name) {
    return find("lower(name) = ?1", name.trim().toLowerCase()).firstResultOptional();
  }

  public List<Watchlist> listByUpdatedAt() {
    return list("order by updatedAt desc, createdAt desc");
  }

  public List<WatchlistItem> listItems(Long watchlistId) {
    return WatchlistItem.list("watchlistId = ?1 order by displayOrder", watchlistId);
  }
}
