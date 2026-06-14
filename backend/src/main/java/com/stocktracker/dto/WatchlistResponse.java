package com.stocktracker.dto;

import java.util.List;

public record WatchlistResponse(List<WatchlistItemView> watchlists) {
  public record WatchlistItemView(
      String id,
      String name,
      List<String> tickers,
      List<WatchlistInstrumentView> instruments,
      String createdAt,
      String updatedAt) {}

  public record WatchlistInstrumentView(
      String symbol, String name, String exchange, String currency) {}
}
