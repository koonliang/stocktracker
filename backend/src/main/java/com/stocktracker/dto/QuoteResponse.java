package com.stocktracker.dto;

import java.time.Instant;
import java.util.List;

public record QuoteResponse(List<QuoteView> quotes) {
  public record QuoteView(
      String symbol,
      Double price,
      String currency,
      Double changeAmount,
      Double changePct,
      Double previousClose,
      Instant asOf,
      Instant fetchedAt,
      String source,
      boolean stale) {}
}
