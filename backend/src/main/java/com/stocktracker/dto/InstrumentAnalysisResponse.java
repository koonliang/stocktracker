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
      double open,
      double high,
      double low,
      double previousClose,
      long volume,
      double week52High,
      double week52Low,
      long marketCap,
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
