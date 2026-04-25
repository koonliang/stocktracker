package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.domain.Watchlist;
import com.stocktracker.domain.WatchlistItem;
import com.stocktracker.dto.WatchlistResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.WatchlistRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class WatchlistService {
  private static final int MAX_NAME_LENGTH = 40;

  @Inject WatchlistRepository watchlistRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject EntityManager entityManager;

  public WatchlistResponse listWatchlists() {
    return new WatchlistResponse(
        watchlistRepository.listByUpdatedAt().stream().map(this::toView).toList());
  }

  @Transactional
  public WatchlistResponse.WatchlistItemView create(String rawName) {
    var name = normalizeName(rawName);
    validateName(name, null);
    var watchlist = new Watchlist();
    watchlist.name = name;
    watchlistRepository.persist(watchlist);
    return toView(watchlist);
  }

  @Transactional
  public WatchlistResponse.WatchlistItemView rename(UUID id, String rawName) {
    var watchlist = getWatchlist(id);
    var name = normalizeName(rawName);
    validateName(name, id);
    watchlist.name = name;
    watchlistRepository.persist(watchlist);
    return toView(watchlist);
  }

  @Transactional
  public void delete(UUID id) {
    var watchlist = getWatchlist(id);
    WatchlistItem.delete("watchlistId", id);
    watchlistRepository.delete(watchlist);
  }

  @Transactional
  public WatchlistResponse.WatchlistItemView addTicker(UUID id, String rawTicker) {
    var watchlist = getWatchlist(id);
    var ticker = rawTicker.trim().toUpperCase(Locale.ROOT);
    if (!instrumentRepository.existsSymbol(ticker)) {
      throw new ApiException(ApiStatuses.UNPROCESSABLE_ENTITY, "validation_error", "Ticker is unknown");
    }
    if (WatchlistItem.count("watchlistId = ?1 and instrumentSymbol = ?2", id, ticker) > 0) {
      throw new ApiException(Status.CONFLICT, "duplicate_ticker", "Ticker already exists");
    }
    var item = new WatchlistItem();
    item.watchlistId = id;
    item.instrumentSymbol = ticker;
    item.displayOrder =
        WatchlistItem.<WatchlistItem>list("watchlistId = ?1 order by displayOrder", id).stream()
                .mapToInt(existing -> existing.displayOrder)
                .max()
                .orElse(-1)
            + 1;
    item.persist();
    watchlist.updatedAt = java.time.LocalDateTime.now();
    return toView(watchlist);
  }

  @Transactional
  public WatchlistResponse.WatchlistItemView removeTicker(UUID id, String rawTicker) {
    var watchlist = getWatchlist(id);
    var ticker = rawTicker.trim().toUpperCase(Locale.ROOT);
    WatchlistItem.delete("watchlistId = ?1 and instrumentSymbol = ?2", id, ticker);
    resequence(id);
    watchlist.updatedAt = java.time.LocalDateTime.now();
    return toView(watchlist);
  }

  @Transactional
  public WatchlistResponse.WatchlistItemView reorder(UUID id, List<String> tickers) {
    var watchlist = getWatchlist(id);
    var existing = watchlistRepository.listItems(id).stream().map(item -> item.instrumentSymbol).toList();
    var normalized = tickers.stream().map(ticker -> ticker.trim().toUpperCase(Locale.ROOT)).toList();
    if (!(existing.size() == normalized.size()
        && existing.containsAll(normalized)
        && normalized.containsAll(existing))) {
      throw new ApiException(Status.BAD_REQUEST, "invalid_order", "Ticker order does not match watchlist items");
    }
    var items = watchlistRepository.listItems(id);
    var offset = items.size();
    for (var item : items) {
      item.displayOrder = item.displayOrder + offset;
    }
    entityManager.flush();
    for (var item : items) {
      item.displayOrder = normalized.indexOf(item.instrumentSymbol);
    }
    watchlist.updatedAt = java.time.LocalDateTime.now();
    return toView(watchlist);
  }

  private Watchlist getWatchlist(UUID id) {
    return watchlistRepository
        .findByIdOptional(id)
        .orElseThrow(() -> new ApiException(Status.NOT_FOUND, "not_found", "Watchlist not found"));
  }

  private void validateName(String name, UUID excludeId) {
    if (name.isBlank()) {
      throw new ApiException(Status.BAD_REQUEST, "validation_error", "Name is required");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new ApiException(Status.BAD_REQUEST, "validation_error", "Name is too long");
    }
    watchlistRepository
        .findByNameIgnoreCase(name)
        .filter(existing -> excludeId == null || !existing.id.equals(excludeId))
        .ifPresent(
            existing -> {
              throw new ApiException(Status.CONFLICT, "duplicate_name", "Watchlist already exists");
            });
  }

  private String normalizeName(String rawName) {
    return rawName == null ? "" : rawName.trim();
  }

  private void resequence(UUID watchlistId) {
    var items = watchlistRepository.listItems(watchlistId);
    for (int index = 0; index < items.size(); index++) {
      items.get(index).displayOrder = index;
    }
  }

  private WatchlistResponse.WatchlistItemView toView(Watchlist watchlist) {
    var items = watchlistRepository.listItems(watchlist.id).stream().map(item -> item.instrumentSymbol).toList();
    return new WatchlistResponse.WatchlistItemView(
        watchlist.id.toString(),
        watchlist.name,
        items,
        watchlist.createdAt.toString(),
        watchlist.updatedAt.toString());
  }
}
