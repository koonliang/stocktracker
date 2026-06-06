package com.stocktracker.persistence;

import com.stocktracker.domain.Watchlist;
import com.stocktracker.domain.WatchlistItem;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WatchlistRepository implements PanacheRepositoryBase<Watchlist, Long> {
  public Optional<Watchlist> findByUserAndNameIgnoreCase(Long userId, String name) {
    return find("userId = ?1 and lower(name) = ?2", userId, name.trim().toLowerCase())
        .firstResultOptional();
  }

  public Optional<Watchlist> findByIdAndUser(Long id, Long userId) {
    return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
  }

  public List<Watchlist> listByUserUpdatedAt(Long userId) {
    return list("userId = ?1 order by updatedAt desc, createdAt desc", userId);
  }

  public List<WatchlistItem> listItems(Long watchlistId) {
    return WatchlistItem.list("watchlistId = ?1 order by displayOrder", watchlistId);
  }
}
