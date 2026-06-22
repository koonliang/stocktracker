package com.stocktracker.scheduler;

import com.stocktracker.service.MarketDataService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Refreshes tracked-symbol price bars on a slower cadence than quote polling. Existing histories
 * are incrementally backfilled from the latest stored trade date and the current-day bar/stat
 * snapshot is updated in place.
 */
@ApplicationScoped
public class PriceHistoryRefreshJob {
  private static final Logger LOG = Logger.getLogger(PriceHistoryRefreshJob.class);

  @Inject QuoteRefreshJob quoteRefreshJob;
  @Inject MarketDataService marketDataService;

  @Scheduled(every = "{stocktracker.marketdata.history-refresh-interval}")
  public void refresh() {
    var symbols = quoteRefreshJob.trackedSymbols();
    if (symbols.isEmpty()) {
      return;
    }
    LOG.infof("Refreshing price history for %d tracked symbols", symbols.size());
    marketDataService.refreshTrackedSymbolsAndAnalysis(symbols);
  }
}
