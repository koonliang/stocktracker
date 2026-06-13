package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.PortfolioTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CostBasisEngineTest {
  private final CostBasisEngine engine = new CostBasisEngine();

  @Test
  void splitsPreserveTotalBasisAndAdjustPerShareBasis() {
    var result =
        engine.replay(
            List.of(
                transaction("2024-01-02", "AAPL", "buy", "10", "100", "5"),
                transaction("2024-02-02", "AAPL", "split", "2", "0", "0"),
                transaction("2024-03-02", "AAPL", "split", "0.1", "0", "0"),
                transaction("2024-04-02", "AAPL", "split", "1.5", "0", "0")));

    assertEquals(0, result.shares("AAPL").compareTo(new BigDecimal("3.0000000000")));
    assertEquals(0, result.costBasis("AAPL").compareTo(new BigDecimal("1005")));
    assertEquals(0, result.averageCost("AAPL").compareTo(new BigDecimal("335.0000000000")));
  }

  @Test
  void sellConsumesSplitAdjustedOpenLotsAndRejectsOversell() {
    var result =
        engine.replay(
            List.of(
                transaction("2024-01-02", "AAPL", "buy", "10", "100", "0"),
                transaction("2024-02-02", "AAPL", "split", "2", "0", "0"),
                transaction("2024-03-02", "AAPL", "sell", "5", "60", "5")));

    assertEquals(0, result.shares("AAPL").compareTo(new BigDecimal("15.0000000000")));
    assertEquals(0, result.costBasis("AAPL").compareTo(new BigDecimal("750.0000000000")));
    assertEquals(1, result.closedLots().size());
    assertEquals(
        0, result.closedLots().getFirst().realizedPnl().compareTo(new BigDecimal("45.0000000000")));

    assertThrows(
        ApiException.class,
        () ->
            engine.replay(
                List.of(
                    transaction("2024-01-02", "AAPL", "buy", "1", "100", "0"),
                    transaction("2024-02-02", "AAPL", "sell", "2", "100", "0"))));
  }

  private PortfolioTransaction transaction(
      String date, String ticker, String type, String quantity, String price, String fees) {
    var transaction = new PortfolioTransaction();
    transaction.tradeDate = LocalDate.parse(date);
    transaction.instrumentSymbol = ticker;
    transaction.transactionType = type;
    transaction.quantity = new BigDecimal(quantity);
    transaction.price = new BigDecimal(price);
    transaction.fees = new BigDecimal(fees);
    return transaction;
  }
}
