package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.pages.AnalysisPage;
import com.stocktracker.e2e.support.BaseTest;
import org.junit.jupiter.api.Test;

/** J3 — Price chart and key statistics display for a seeded instrument (Story 2 AS-3). */
class AnalysisJourneyTest extends BaseTest {

  /** AAPL is a seeded instrument with price history and stats. */
  private static final String TICKER = "AAPL";

  @Test
  void analysisShowsChartAndKeyStats() {
    signInAsSeedUser();
    open("/analysis/" + TICKER);
    AnalysisPage analysis = new AnalysisPage(driver, waits).waitLoaded();

    assertThat(analysis.isPriceChartVisible()).isTrue();
    assertThat(analysis.keyStatsText()).isNotBlank();
  }
}
