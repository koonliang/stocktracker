package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stocktracker.domain.PortfolioTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LotMatchingTest {
  private final CostBasisEngine engine = new CostBasisEngine();

  @Test
  void realizedLotsRespectFifoAndLifoWithSplits() {
    var transactions =
        List.of(
            tx("2024-01-01", "AAPL", "buy", "10", "100"),
            tx("2024-02-01", "AAPL", "buy", "10", "120"),
            tx("2024-03-01", "AAPL", "split", "2", "0"),
            tx("2024-04-01", "AAPL", "sell", "10", "80"));

    var fifo = engine.replay(transactions, CostBasisEngine.MatchingMethod.FIFO);
    var lifo = engine.replay(transactions, CostBasisEngine.MatchingMethod.LIFO);

    assertEquals(
        0, fifo.closedLots().getFirst().realizedPnl().compareTo(new BigDecimal("300.0000000000")));
    assertEquals(
        0, lifo.closedLots().getFirst().realizedPnl().compareTo(new BigDecimal("200.0000000000")));
  }

  private PortfolioTransaction tx(
      String date, String symbol, String type, String qty, String price) {
    var transaction = new PortfolioTransaction();
    transaction.tradeDate = LocalDate.parse(date);
    transaction.instrumentSymbol = symbol;
    transaction.transactionType = type;
    transaction.quantity = new BigDecimal(qty);
    transaction.price = new BigDecimal(price);
    transaction.fees = BigDecimal.ZERO;
    return transaction;
  }
}
