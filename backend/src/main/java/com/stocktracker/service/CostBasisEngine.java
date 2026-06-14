package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.api.ApiStatuses;
import com.stocktracker.domain.PortfolioTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class CostBasisEngine {
  private static final int SCALE = 10;

  public Result replay(List<PortfolioTransaction> transactions) {
    return replay(transactions, MatchingMethod.FIFO);
  }

  public Result replay(List<PortfolioTransaction> transactions, MatchingMethod method) {
    var lots = new ArrayList<Lot>();
    var closed = new ArrayList<ClosedLot>();
    var ordered =
        transactions.stream()
            .filter(transaction -> transaction.instrumentSymbol != null)
            .sorted(
                Comparator.comparing((PortfolioTransaction transaction) -> transaction.tradeDate)
                    .thenComparing(transaction -> transaction.id == null ? 0L : transaction.id))
            .toList();

    for (var transaction : ordered) {
      switch (transaction.transactionType) {
        case "buy" -> lots.add(openLot(transaction));
        case "sell" -> closeLots(transaction, lots, closed, method);
        case "split" -> applySplit(transaction, lots);
        default -> {
          // dividend and cash movements do not affect cost basis.
        }
      }
    }

    return new Result(List.copyOf(lots), List.copyOf(closed));
  }

  private Lot openLot(PortfolioTransaction transaction) {
    var fees = transaction.fees == null ? BigDecimal.ZERO : transaction.fees;
    var totalCost = transaction.quantity.multiply(transaction.price).add(fees);
    return new Lot(
        transaction.instrumentSymbol,
        transaction.tradeDate,
        transaction.quantity,
        totalCost,
        totalCost.divide(transaction.quantity, SCALE, RoundingMode.HALF_UP));
  }

  private void closeLots(
      PortfolioTransaction sell,
      List<Lot> openLots,
      List<ClosedLot> closedLots,
      MatchingMethod method) {
    var remaining = sell.quantity;
    var proceeds = sell.quantity.multiply(sell.price);
    var feePerShare =
        sell.fees == null || sell.fees.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : sell.fees.divide(sell.quantity, SCALE, RoundingMode.HALF_UP);

    for (int index = firstIndex(openLots, method);
        index >= 0 && index < openLots.size() && remaining.compareTo(BigDecimal.ZERO) > 0; ) {
      var lot = openLots.get(index);
      if (!lot.symbol().equalsIgnoreCase(sell.instrumentSymbol)) {
        index = nextIndex(index, method);
        continue;
      }
      var closingQuantity = remaining.min(lot.quantity());
      var costBasis =
          lot.unitCost().multiply(closingQuantity).setScale(SCALE, RoundingMode.HALF_UP);
      var lotProceeds =
          sell.price
              .subtract(feePerShare)
              .multiply(closingQuantity)
              .setScale(SCALE, RoundingMode.HALF_UP);
      closedLots.add(
          new ClosedLot(
              sell.instrumentSymbol,
              lot.openedOn(),
              sell.tradeDate,
              closingQuantity,
              costBasis,
              lotProceeds,
              lotProceeds.subtract(costBasis)));

      var updatedQuantity = lot.quantity().subtract(closingQuantity);
      remaining = remaining.subtract(closingQuantity);
      if (updatedQuantity.compareTo(BigDecimal.ZERO) == 0) {
        openLots.remove(index);
        if (method == MatchingMethod.LIFO) {
          index--;
        }
      } else {
        openLots.set(
            index,
            new Lot(
                lot.symbol(),
                lot.openedOn(),
                updatedQuantity,
                lot.unitCost().multiply(updatedQuantity).setScale(SCALE, RoundingMode.HALF_UP),
                lot.unitCost()));
        index = nextIndex(index, method);
      }
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      throw new ApiException(
          ApiStatuses.UNPROCESSABLE_ENTITY,
          "validation_error",
          "sell quantity exceeds held shares");
    }
  }

  private int firstIndex(List<Lot> lots, MatchingMethod method) {
    return method == MatchingMethod.LIFO ? lots.size() - 1 : 0;
  }

  private int nextIndex(int current, MatchingMethod method) {
    return method == MatchingMethod.LIFO ? current - 1 : current + 1;
  }

  public enum MatchingMethod {
    FIFO,
    LIFO,
    SPECIFIC;

    public static MatchingMethod parse(String value) {
      if (value == null || value.isBlank()) {
        return FIFO;
      }
      return switch (value.toLowerCase(java.util.Locale.ROOT)) {
        case "fifo" -> FIFO;
        case "lifo" -> LIFO;
        case "specific" -> SPECIFIC;
        default ->
            throw new ApiException(
                ApiStatuses.UNPROCESSABLE_ENTITY,
                "validation_error",
                "method must be fifo, lifo, or specific");
      };
    }
  }

  private void applySplit(PortfolioTransaction split, List<Lot> openLots) {
    var ratio = split.quantity;
    for (int i = 0; i < openLots.size(); i++) {
      var lot = openLots.get(i);
      if (!lot.symbol().equalsIgnoreCase(split.instrumentSymbol)) {
        continue;
      }
      var newQuantity = lot.quantity().multiply(ratio).setScale(SCALE, RoundingMode.HALF_UP);
      var newUnitCost = lot.totalCost().divide(newQuantity, SCALE, RoundingMode.HALF_UP);
      openLots.set(
          i, new Lot(lot.symbol(), lot.openedOn(), newQuantity, lot.totalCost(), newUnitCost));
    }
  }

  public record Result(List<Lot> openLots, List<ClosedLot> closedLots) {
    public BigDecimal shares(String symbol) {
      return openLots.stream()
          .filter(lot -> lot.symbol().equalsIgnoreCase(symbol))
          .map(Lot::quantity)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal costBasis(String symbol) {
      return openLots.stream()
          .filter(lot -> lot.symbol().equalsIgnoreCase(symbol))
          .map(Lot::totalCost)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal averageCost(String symbol) {
      var shares = shares(symbol);
      if (shares.compareTo(BigDecimal.ZERO) == 0) {
        return BigDecimal.ZERO;
      }
      return costBasis(symbol).divide(shares, SCALE, RoundingMode.HALF_UP);
    }
  }

  public record Lot(
      String symbol,
      java.time.LocalDate openedOn,
      BigDecimal quantity,
      BigDecimal totalCost,
      BigDecimal unitCost) {}

  public record ClosedLot(
      String symbol,
      java.time.LocalDate openedOn,
      java.time.LocalDate closedOn,
      BigDecimal quantity,
      BigDecimal costBasis,
      BigDecimal proceeds,
      BigDecimal realizedPnl) {}
}
