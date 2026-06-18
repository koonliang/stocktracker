package com.stocktracker.e2e.journeys;

import com.stocktracker.e2e.pages.AlertsPage;
import com.stocktracker.e2e.support.BaseTest;
import org.junit.jupiter.api.Test;

class AlertsTest extends BaseTest {
  @Test
  void alertsRouteCreatesAnAlert() {
    signInAsSeedUser();
    open("/alerts");
    new AlertsPage(driver, waits).waitLoaded().createPriceAlert("AAPL", "200");
  }

  @Test
  void createMultiplePriceAlerts() {
    signInAsSeedUser();
    open("/alerts");
    AlertsPage page = new AlertsPage(driver, waits).waitLoaded();
    page.createPriceAlert("AAPL", "200");
    page.createPriceAlert("MSFT", "150");
  }
}
