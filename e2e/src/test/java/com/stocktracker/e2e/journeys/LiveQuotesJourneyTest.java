package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.pages.LiveQuotesPage;
import com.stocktracker.e2e.support.BaseTest;
import org.junit.jupiter.api.Test;

/**
 * US1 — live, auto-updating, multi-currency dashboard with global-symbol add (SC-001/010/012).
 * Runs against the deterministic stub provider, so quotes are reproducible and network-free.
 */
class LiveQuotesJourneyTest extends BaseTest {

  @Test
  void dashboardShowsLastUpdatedAndBaseCurrencyControls() {
    signInAsSeedUser();
    open("/");
    LiveQuotesPage page = new LiveQuotesPage(driver, waits).waitLoaded();

    assertThat(page.lastUpdatedText()).containsIgnoringCase("last updated");
    assertThat(page.hasBaseCurrencySelect()).isTrue();
  }

  @Test
  void dashboardOmitsLegacySymbolSearchControl() {
    signInAsSeedUser();
    open("/");
    LiveQuotesPage page = new LiveQuotesPage(driver, waits).waitLoaded();

    assertThat(page.hasSymbolSearch()).isFalse();
  }
}
