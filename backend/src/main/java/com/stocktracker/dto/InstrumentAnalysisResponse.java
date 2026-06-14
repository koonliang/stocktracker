package com.stocktracker.dto;

import java.util.List;

public record InstrumentAnalysisResponse(
    TickerView ticker,
    StatsView stats,
    QuoteResponse.QuoteView quote,
    List<PriceHistoryPoint> priceHistory,
    PositionSummary positionSummary) {
  public record TickerView(String symbol, String name, String sector, String exchange) {}

  public record StatsView(
      Double open,
      Double high,
      Double low,
      Double previousClose,
      Long volume,
      Double week52High,
      Double week52Low,
      Long marketCap,
      Double peRatio) {}

  public record PriceHistoryPoint(
      String date, double open, double high, double low, double close, long volume) {}

  public record PositionSummary(
      double shares,
      double averageCost,
      double marketValue,
      double unrealizedPnL,
      double unrealizedPnLPct) {}
}
