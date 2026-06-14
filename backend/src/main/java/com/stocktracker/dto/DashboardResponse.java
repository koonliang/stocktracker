package com.stocktracker.dto;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(Summary summary, List<Holding> holdings) {
  /** Combined totals are expressed in the user's {@code baseCurrency} (FR-031). */
  public record Summary(
      double totalMarketValue,
      double totalCostBasis,
      double totalUnrealizedPnL,
      double totalUnrealizedPnLPct,
      double totalDayChange,
      double totalDayChangePct,
      String baseCurrency) {}

  public record Holding(
      String ticker,
      String name,
      double shares,
      // Native (the instrument's own currency).
      String currency,
      double averageCost,
      double nativePrice,
      double nativeMarketValue,
      // Base-converted (the user's reporting currency).
      double costBasis,
      double currentPrice,
      double marketValue,
      double unrealizedPnL,
      double unrealizedPnLPct,
      double dayChange,
      double dayChangePct,
      double weight,
      // Freshness for the "last updated" / stale indicators (FR-005).
      Instant asOf,
      Instant fetchedAt,
      boolean stale) {}
}
