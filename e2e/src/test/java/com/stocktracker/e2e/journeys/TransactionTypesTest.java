package com.stocktracker.e2e.journeys;

import com.stocktracker.e2e.pages.TransactionsPage;
import com.stocktracker.e2e.support.BaseTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/** US2 — records split transactions and keeps v1 CSV import working. */
class TransactionTypesTest extends BaseTest {
  @Test
  void recordSplitAndImportV1Csv() throws Exception {
    Path v1Csv = Paths.get(getClass().getResource("/transactions-sample.csv").toURI());

    signInAsSeedUser();
    open("/transactions");
    TransactionsPage transactions = new TransactionsPage(driver, waits);

    transactions.recordBuy("AAPL", "10", "100");
    transactions.recordSplit("AAPL", "2");
    transactions.importCsv(v1Csv);
    transactions.waitForTicker("TSLA");
  }
}
