package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.pages.WatchlistsPage;
import com.stocktracker.e2e.support.BaseTest;
import org.junit.jupiter.api.Test;

/** J2 — An instrument can be added to and removed from a watchlist (Story 2 AS-2). */
class WatchlistJourneyTest extends BaseTest {

  private static final String TICKER = "TSLA";

  @Test
  void addAndRemoveInstrument() {
    // Unique name so repeated local runs against a persistent DB don't collide.
    String name = "E2E " + System.currentTimeMillis();

    open("/watchlists");
    WatchlistsPage watchlists = new WatchlistsPage(driver, waits).createWatchlist(name);

    watchlists.addTicker(TICKER);
    assertThat(watchlists.waitForItem(TICKER).isDisplayed()).isTrue();

    watchlists.removeItem(TICKER);
    assertThat(watchlists.waitForItemGone(TICKER)).isTrue();
  }
}
