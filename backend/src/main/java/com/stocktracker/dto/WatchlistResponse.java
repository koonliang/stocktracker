package com.stocktracker.dto;

import java.util.List;

public record WatchlistResponse(List<WatchlistItemView> watchlists) {
  public record WatchlistItemView(
      String id, String name, List<String> tickers, String createdAt, String updatedAt) {}
}
