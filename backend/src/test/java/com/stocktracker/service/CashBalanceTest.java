package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestSecurity(user = "seed@stocktracker.local")
@JwtSecurity(
    claims = {
      @Claim(key = "sub", value = "1"),
      @Claim(key = "email", value = "seed@stocktracker.local")
    })
class CashBalanceTest extends IntegrationTestSupport {
  @Inject CashBalanceService cashBalanceService;

  @Test
  void calculatesPerCurrencyRunningCashBalance() {
    var balances =
        cashBalanceService.balances(
            List.of(
                transaction("deposit", null, "0", "0", "0", "1000", "USD"),
                transaction("buy", "AAPL", "2", "100", "1", null, "USD"),
                transaction("dividend", "AAPL", "0", "0", "0.50", "12.50", "USD"),
                transaction("sell", "AAPL", "1", "110", "1", null, "USD"),
                transaction("withdrawal", null, "0", "0", "0", "50", "USD"),
                transaction("fee", null, "0", "0", "0", "2", "USD"),
                transaction("deposit", null, "0", "0", "0", "100", "SGD")));

    assertEquals(0, balances.get("USD").compareTo(new BigDecimal("868.00")));
    assertEquals(0, balances.get("SGD").compareTo(new BigDecimal("100")));
  }

  private PortfolioTransaction transaction(
      String type,
      String ticker,
      String quantity,
      String price,
      String fees,
      String amount,
      String currency) {
    var transaction = new PortfolioTransaction();
    transaction.tradeDate = LocalDate.parse("2024-01-01");
    transaction.instrumentSymbol = ticker;
    transaction.transactionType = type;
    transaction.quantity = new BigDecimal(quantity);
    transaction.price = new BigDecimal(price);
    transaction.fees = new BigDecimal(fees);
    transaction.amount = amount == null ? null : new BigDecimal(amount);
    transaction.currency = currency;
    return transaction;
  }
}
