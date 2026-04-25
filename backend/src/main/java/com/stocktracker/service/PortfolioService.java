package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.dto.DashboardResponse;
import com.stocktracker.dto.TransactionRequest;
import com.stocktracker.dto.TransactionResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.PortfolioTransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response.Status;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class PortfolioService {
  @Inject PortfolioTransactionRepository transactionRepository;
  @Inject InstrumentRepository instrumentRepository;
  @Inject TransactionValidationService transactionValidationService;

  public DashboardResponse getDashboard() {
    var transactions = transactionRepository.listAscending();
    return buildDashboard(transactions);
  }

  public List<TransactionResponse> listTransactions() {
    return transactionRepository.listDescending().stream().map(this::toResponse).toList();
  }

  public Map<String, BigDecimal> currentShareBalances() {
    var balances = new LinkedHashMap<String, BigDecimal>();
    for (var transaction : transactionRepository.listAscending()) {
      var ticker = transaction.instrumentSymbol;
      var quantity = transaction.quantity;
      var current = balances.getOrDefault(ticker, BigDecimal.ZERO);
      if ("buy".equals(transaction.transactionType)) {
        balances.put(ticker, current.add(quantity));
      } else {
        balances.put(ticker, current.subtract(quantity));
      }
    }
    return balances;
  }

  @Transactional
  public void createTransactions(List<TransactionRequest> requests, String source) {
    var normalized = requests.stream().map(transactionValidationService::normalize).toList();
    transactionValidationService.validateBatch(normalized, currentShareBalances());
    for (var request : normalized) {
      var transaction = new PortfolioTransaction();
      transaction.tradeDate = request.date();
      transaction.instrumentSymbol = request.ticker();
      transaction.transactionType = request.type();
      transaction.quantity = request.quantity();
      transaction.price = request.price();
      transaction.fees = request.fees();
      transaction.source = source;
      transactionRepository.persist(transaction);
    }
  }

  @Transactional
  public DashboardResponse deleteTransaction(UUID transactionId) {
    var transaction =
        transactionRepository
            .findByIdOptional(transactionId)
            .orElseThrow(
                () ->
                    new ApiException(
                        Status.NOT_FOUND, "not_found", "Transaction does not exist"));
    transactionRepository.delete(transaction);
    return getDashboard();
  }

  public DashboardResponse buildDashboard(List<PortfolioTransaction> transactions) {
    var symbols =
        transactions.stream().map(transaction -> transaction.instrumentSymbol).collect(Collectors.toSet());
    if (symbols.isEmpty()) {
      return new DashboardResponse(new DashboardResponse.Summary(0, 0, 0, 0, 0, 0), List.of());
    }

    var instruments = instrumentRepository.findBySymbols(symbols);
    var barsBySymbol = groupBars(instrumentRepository.listPriceBars(symbols));
    var positions = new LinkedHashMap<String, PositionAccumulator>();

    for (var transaction : transactions) {
      var accumulator =
          positions.computeIfAbsent(
              transaction.instrumentSymbol, ignored -> new PositionAccumulator());
      if ("buy".equals(transaction.transactionType)) {
        var newShares = accumulator.shares.add(transaction.quantity);
        var addedCost = transaction.quantity.multiply(transaction.price).add(transaction.fees);
        accumulator.averageCost =
            newShares.compareTo(BigDecimal.ZERO) > 0
                ? accumulator.totalCost.add(addedCost).divide(newShares, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        accumulator.totalCost = accumulator.totalCost.add(addedCost);
        accumulator.shares = newShares;
      } else {
        var soldShares = transaction.quantity.min(accumulator.shares);
        accumulator.totalCost =
            accumulator.totalCost.subtract(
                soldShares.multiply(accumulator.averageCost).setScale(8, RoundingMode.HALF_UP));
        accumulator.shares = accumulator.shares.subtract(soldShares);
        if (accumulator.shares.compareTo(BigDecimal.ZERO) <= 0) {
          accumulator.shares = BigDecimal.ZERO;
          accumulator.averageCost = BigDecimal.ZERO;
          accumulator.totalCost = BigDecimal.ZERO;
        }
      }
    }

    List<DashboardResponse.Holding> holdings = new ArrayList<>();
    var totalMarketValue = BigDecimal.ZERO;
    var totalCostBasis = BigDecimal.ZERO;
    var totalDayChange = BigDecimal.ZERO;

    for (var entry : positions.entrySet()) {
      if (entry.getValue().shares.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      var symbol = entry.getKey();
      var bars = barsBySymbol.getOrDefault(symbol, List.of());
      var currentPrice = latestClose(bars);
      var previousClose = previousClose(bars, currentPrice);
      var shares = entry.getValue().shares;
      var costBasis = shares.multiply(entry.getValue().averageCost);
      var marketValue = shares.multiply(currentPrice);
      var unrealizedPnL = marketValue.subtract(costBasis);
      var dayChange = shares.multiply(currentPrice.subtract(previousClose));

      totalMarketValue = totalMarketValue.add(marketValue);
      totalCostBasis = totalCostBasis.add(costBasis);
      totalDayChange = totalDayChange.add(dayChange);

      holdings.add(
          new DashboardResponse.Holding(
              symbol,
              instruments.containsKey(symbol) ? instruments.get(symbol).name : symbol,
              scale6(shares),
              scale4(entry.getValue().averageCost),
              scale4(costBasis),
              scale4(currentPrice),
              scale4(marketValue),
              scale4(unrealizedPnL),
              ratio(unrealizedPnL, costBasis),
              scale4(dayChange),
              ratio(currentPrice.subtract(previousClose), previousClose),
              0));
    }

    var portfolioMarketValue = totalMarketValue;
    holdings =
        holdings.stream()
            .sorted((left, right) -> Double.compare(right.marketValue(), left.marketValue()))
            .map(
                holding ->
                    new DashboardResponse.Holding(
                        holding.ticker(),
                        holding.name(),
                        holding.shares(),
                        holding.averageCost(),
                        holding.costBasis(),
                        holding.currentPrice(),
                        holding.marketValue(),
                        holding.unrealizedPnL(),
                        holding.unrealizedPnLPct(),
                        holding.dayChange(),
                        holding.dayChangePct(),
                        portfolioMarketValue.compareTo(BigDecimal.ZERO) > 0
                            ? scale4(
                                BigDecimal.valueOf(holding.marketValue())
                                    .divide(portfolioMarketValue, 8, RoundingMode.HALF_UP))
                            : 0))
            .toList();

    var totalUnrealizedPnL = totalMarketValue.subtract(totalCostBasis);
    var previousPortfolioValue = totalMarketValue.subtract(totalDayChange);

    return new DashboardResponse(
        new DashboardResponse.Summary(
            scale4(totalMarketValue),
            scale4(totalCostBasis),
            scale4(totalUnrealizedPnL),
            ratio(totalUnrealizedPnL, totalCostBasis),
            scale4(totalDayChange),
            ratio(totalDayChange, previousPortfolioValue)),
        holdings);
  }

  public PositionSnapshot findPosition(String symbol) {
    var dashboard = getDashboard();
    return dashboard.holdings().stream()
        .filter(holding -> holding.ticker().equalsIgnoreCase(symbol))
        .findFirst()
        .map(
            holding ->
                new PositionSnapshot(
                    holding.shares(),
                    holding.averageCost(),
                    holding.marketValue(),
                    holding.unrealizedPnL(),
                    holding.unrealizedPnLPct()))
        .orElse(null);
  }

  private TransactionResponse toResponse(PortfolioTransaction transaction) {
    return new TransactionResponse(
        transaction.id.toString(),
        transaction.tradeDate.toString(),
        transaction.instrumentSymbol,
        transaction.transactionType,
        scale6(transaction.quantity),
        scale4(transaction.price),
        scale4(transaction.fees),
        transaction.source);
  }

  private Map<String, List<InstrumentPriceBar>> groupBars(List<InstrumentPriceBar> bars) {
    var grouped = new HashMap<String, List<InstrumentPriceBar>>();
    for (var bar : bars) {
      grouped.computeIfAbsent(bar.instrumentSymbol, ignored -> new ArrayList<>()).add(bar);
    }
    return grouped;
  }

  private BigDecimal latestClose(List<InstrumentPriceBar> bars) {
    if (bars.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return bars.get(bars.size() - 1).closePrice;
  }

  private BigDecimal previousClose(List<InstrumentPriceBar> bars, BigDecimal fallback) {
    if (bars.size() < 2) {
      return fallback;
    }
    return bars.get(bars.size() - 2).closePrice;
  }

  private static double ratio(BigDecimal numerator, BigDecimal denominator) {
    if (denominator.compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    }
    return numerator.divide(denominator, 8, RoundingMode.HALF_UP).doubleValue();
  }

  private static double scale4(BigDecimal value) {
    return value.setScale(4, RoundingMode.HALF_UP).doubleValue();
  }

  private static double scale6(BigDecimal value) {
    return value.setScale(6, RoundingMode.HALF_UP).doubleValue();
  }

  private static final class PositionAccumulator {
    private BigDecimal shares = BigDecimal.ZERO;
    private BigDecimal averageCost = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;
  }

  public record PositionSnapshot(
      double shares,
      double averageCost,
      double marketValue,
      double unrealizedPnL,
      double unrealizedPnLPct) {}
}
