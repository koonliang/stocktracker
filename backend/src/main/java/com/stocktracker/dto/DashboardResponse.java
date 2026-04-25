package com.stocktracker.dto;

import java.util.List;

public record DashboardResponse(Summary summary, List<Holding> holdings) {
  public record Summary(
      double totalMarketValue,
      double totalCostBasis,
      double totalUnrealizedPnL,
      double totalUnrealizedPnLPct,
      double totalDayChange,
      double totalDayChangePct) {}

  public record Holding(
      String ticker,
      String name,
      double shares,
      double averageCost,
      double costBasis,
      double currentPrice,
      double marketValue,
      double unrealizedPnL,
      double unrealizedPnLPct,
      double dayChange,
      double dayChangePct,
      double weight) {}
}
