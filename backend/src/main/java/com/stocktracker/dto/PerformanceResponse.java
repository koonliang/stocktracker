package com.stocktracker.dto;

import java.util.List;

public record PerformanceResponse(
    String window,
    String method,
    String baseCurrency,
    double realizedPnL,
    double unrealizedPnL,
    double timeWeightedReturnPct,
    List<ClosedLotView> closedLots,
    List<IncomeEventView> incomeEvents,
    List<ReturnPoint> returnSeries,
    List<ContributionView> contributions) {
  public record ClosedLotView(
      String symbol,
      String currency,
      String openDate,
      String closeDate,
      double quantity,
      double costBasisNative,
      double proceedsNative,
      double realizedPnLNative,
      double realizedPnLBase,
      ConversionDtos.ConversionMetadata realizedPnlConversion) {}

  public record IncomeEventView(
      String symbol,
      String currency,
      String date,
      String type,
      double amountNative,
      double amountBase,
      ConversionDtos.ConversionMetadata amountConversion) {}

  public record ReturnPoint(String date, double cumulativeReturnPct) {}

  public record ContributionView(
      String symbol,
      double contributionPct,
      double contributionBase,
      ConversionDtos.ConversionMetadata contributionConversion) {}
}
