package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class PortfolioServiceIT extends IntegrationTestSupport {
  @Inject PortfolioService portfolioService;

  @Test
  void aggregatesSharesAndAverageCostAcrossBuysAndSells() throws Exception {
    persistTransaction("2024-01-10", "NVDA", "buy", "10", "100.0000", "0.0000");
    persistTransaction("2024-02-10", "NVDA", "buy", "5", "120.0000", "0.0000");
    persistTransaction("2024-03-10", "NVDA", "sell", "4", "130.0000", "0.0000");

    var dashboard = portfolioService.getDashboard();
    var holding = dashboard.holdings().getFirst();

    assertEquals(1, dashboard.holdings().size());
    assertEquals("NVDA", holding.ticker());
    assertEquals(11.0, holding.shares(), 0.0001);
    assertEquals(109.0909, holding.averageCost(), 0.0002);
    assertEquals(1200.0000, holding.costBasis(), 0.0002);
  }
}
