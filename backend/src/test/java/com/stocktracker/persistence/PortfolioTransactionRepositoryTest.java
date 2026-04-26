package com.stocktracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class PortfolioTransactionRepositoryTest extends IntegrationTestSupport {
  @Inject PortfolioTransactionRepository portfolioTransactionRepository;

  @Test
  void ordersTransactionsAscendingAndDescendingByTradeDate() throws Exception {
    persistTransaction("2024-01-10", "AAPL", "buy", "1", "100.0000", "0.0000");
    persistTransaction("2024-03-10", "MSFT", "buy", "1", "200.0000", "0.0000");
    persistTransaction("2024-02-10", "NVDA", "buy", "1", "300.0000", "0.0000");

    var ascending = portfolioTransactionRepository.listAscending();
    var descending = portfolioTransactionRepository.listDescending();

    assertEquals("AAPL", ascending.get(0).instrumentSymbol);
    assertEquals("MSFT", ascending.get(2).instrumentSymbol);
    assertEquals("MSFT", descending.get(0).instrumentSymbol);
    assertEquals("AAPL", descending.get(2).instrumentSymbol);
  }
}
