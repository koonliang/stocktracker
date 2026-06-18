package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.pages.DashboardPage;
import com.stocktracker.e2e.pages.LiveQuotesPage;
import com.stocktracker.e2e.pages.PerformancePage;
import com.stocktracker.e2e.support.BaseTest;
import org.junit.jupiter.api.Test;

class PerformanceTest extends BaseTest {
  @Test
  void performanceRouteRendersChartTablesAndControls() {
    signInAsSeedUser();
    open("/performance");
    var page = new PerformancePage(driver, waits).waitLoaded();

    page.chooseAllWindow();
    page.chooseLifo();
  }

  @Test
  void baseCurrencySwitchRefreshesDashboardAndPerformanceCurrencyViews() {
    signInAsSeedUser();
    open("/");
    new LiveQuotesPage(driver, waits).waitLoaded().selectBaseCurrency("SGD");
    var dashboard = new DashboardPage(driver, waits).waitLoaded();

    assertThat(dashboard.summaryTilesText()).contains("SGD");
    assertThat(dashboard.holdingsText()).containsAnyOf("SGD", "Stale rate", "Rate unavailable");

    open("/performance");
    var performance = new PerformancePage(driver, waits).waitLoaded();

    assertThat(performance.pageText()).containsAnyOf("SGD", "Stale rate", "Rate unavailable");
  }
}
