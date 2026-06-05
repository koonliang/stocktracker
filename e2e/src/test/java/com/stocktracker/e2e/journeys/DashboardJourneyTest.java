package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.pages.DashboardPage;
import com.stocktracker.e2e.support.BaseTest;
import org.junit.jupiter.api.Test;

/** J1 — Dashboard renders seeded holdings and a non-empty portfolio summary (Story 2 AS-1). */
class DashboardJourneyTest extends BaseTest {

  @Test
  void dashboardShowsHoldingsAndSummary() {
    open("/");
    DashboardPage dashboard = new DashboardPage(driver, waits).waitLoaded();

    assertThat(dashboard.holdingsRowCount()).isGreaterThanOrEqualTo(1);
    assertThat(dashboard.summaryTilesText()).isNotBlank();
  }
}
