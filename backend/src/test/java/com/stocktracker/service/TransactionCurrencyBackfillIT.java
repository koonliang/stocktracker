package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stocktracker.persistence.PortfolioTransactionRepository;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class TransactionCurrencyBackfillIT extends IntegrationTestSupport {
  @Inject TransactionCurrencyBackfillService backfillService;
  @Inject PortfolioTransactionRepository transactionRepository;

  @Test
  void backfillSetsCurrencyFromInstrumentForSecurityTransactions() throws Exception {
    persistLegacyTransaction("2024-01-15", "AAPL", "buy", "10", "150", "0");

    inTransaction(
        () -> {
          var tx = transactionRepository.findMissingCurrency(SEED_USER_ID).stream().findFirst();
          backfillService.backfill(tx.get(), "USD");
        });

    inTransaction(
        () -> {
          var tx = transactionRepository.listAscending(SEED_USER_ID).stream().findFirst();
          assertEquals("USD", tx.get().currency);
          assertEquals("instrument", tx.get().currencySource);
        });
  }

  @Test
  void backfillSetsBaseCurrencyForCashOnlyTransactions() throws Exception {
    persistLegacyTransaction("2024-01-15", null, "deposit", "0", "0", "0", "1000");

    inTransaction(
        () -> {
          var tx = transactionRepository.findMissingCurrency(SEED_USER_ID).stream().findFirst();
          backfillService.backfill(tx.get(), "USD");
        });

    inTransaction(
        () -> {
          var tx = transactionRepository.listAscending(SEED_USER_ID).stream().findFirst();
          assertEquals("USD", tx.get().currency);
          assertEquals("user_base_backfill", tx.get().currencySource);
        });
  }

  @Test
  void backfillDoesNotOverwriteExistingCurrency() throws Exception {
    persistTransaction("2024-01-15", "AAPL", "buy", "10", "150", "0", null, "SGD");

    inTransaction(
        () -> {
          var tx = transactionRepository.listAscending(SEED_USER_ID).stream().findFirst();
          backfillService.backfill(tx.get(), "USD");
          assertEquals("SGD", tx.get().currency);
        });
  }
}
