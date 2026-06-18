package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.pages.TransactionsPage;
import com.stocktracker.e2e.support.BaseTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/** J4 — Transactions import from CSV and export to CSV (Story 2 AS-4, US3 currency round-trip). */
class CsvImportExportJourneyTest extends BaseTest {

  /**
   * TSLA is a seeded instrument but has no seeded holdings, so its row proves the import landed.
   */
  private static final String IMPORTED_TICKER = "TSLA";

  @Test
  void importThenExport() throws Exception {
    Path csv = Paths.get(getClass().getResource("/transactions-sample.csv").toURI());

    signInAsSeedUser();
    open("/transactions");
    TransactionsPage transactions = new TransactionsPage(driver, waits);

    transactions.importCsv(csv);
    transactions.waitForTicker(IMPORTED_TICKER);

    transactions.export();
    assertThat(transactions.waitForExportedCsv()).isTrue();
  }

  @Test
  void v2ImportWithCurrencyRoundTrip() throws Exception {
    Path csv = Paths.get(getClass().getResource("/transactions-v2-sample.csv").toURI());

    signInAsSeedUser();
    open("/transactions");
    TransactionsPage transactions = new TransactionsPage(driver, waits);

    transactions.importCsv(csv);
    transactions.waitForTicker(IMPORTED_TICKER);

    transactions.export();
    assertThat(transactions.waitForExportedCsv()).isTrue();
  }
}
