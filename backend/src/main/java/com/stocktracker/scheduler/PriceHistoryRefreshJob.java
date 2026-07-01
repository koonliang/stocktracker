package com.stocktracker.scheduler;

import com.stocktracker.bootstrap.DevDataBootstrap;
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

  @Inject DevDataBootstrap devDataBootstrap;
  @Inject QuoteRefreshJob quoteRefreshJob;
  @Inject MarketDataService marketDataService;

  @Scheduled(every = "{stocktracker.marketdata.history-refresh-interval}")
  public void refresh() {
    if (devDataBootstrap.isBootstrappingMarketData()) {
      LOG.debug("Skipping price history refresh while dev bootstrap is hydrating market data");
      return;
    }
    var symbols = quoteRefreshJob.trackedSymbols();
    if (symbols.isEmpty()) {
      return;
    }
    LOG.infof("Refreshing price history for %d tracked symbols", symbols.size());
    var startedAtNanos = System.nanoTime();
    try {
      marketDataService.refreshTrackedSymbolsAndAnalysis(symbols);
    } finally {
      var elapsedMillis =
          java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
      LOG.infof(
          "Price history refresh finished for %d tracked symbols in %d ms",
          symbols.size(), elapsedMillis);
    }
  }
}
