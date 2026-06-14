package com.stocktracker.e2e.journeys;

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
}
