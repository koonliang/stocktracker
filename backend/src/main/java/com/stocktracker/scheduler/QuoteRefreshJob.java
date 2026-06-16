package com.stocktracker.scheduler;

import com.stocktracker.service.QuoteCacheService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * Refreshes the quote cache on a fixed cadence for the union of tracked symbols (any non-cash
 * {@code portfolio_transaction} ∪ any {@code watchlist_item}). Runs continuously regardless of any
 * single market's hours; per-symbol freshness is governed by {@code fetched_at} (FR-028). Alert
 * evaluation hooks in here in US4.
 */
@ApplicationScoped
public class QuoteRefreshJob {
  private static final Logger LOG = Logger.getLogger(QuoteRefreshJob.class);

  @Inject QuoteCacheService quoteCacheService;
  @Inject EntityManager entityManager;

  @Scheduled(every = "{stocktracker.marketdata.refresh-interval}")
  public void refresh() {
    var symbols = trackedSymbols();
    if (symbols.isEmpty()) {
      return;
    }
    LOG.debugf("Refreshing %d tracked symbols", symbols.size());
    quoteCacheService.refreshSymbols(symbols);
  }

  /** Distinct symbols held (non-cash transactions) or watched, across all users. */
  public Set<String> trackedSymbols() {
    var symbols = new LinkedHashSet<String>();
    symbols.addAll(
        entityManager
            .createQuery(
                "select distinct t.instrumentSymbol from PortfolioTransaction t"
                    + " where t.instrumentSymbol is not null",
                String.class)
            .getResultList());
    symbols.addAll(
        entityManager
            .createQuery("select distinct i.instrumentSymbol from WatchlistItem i", String.class)
            .getResultList());
    return symbols;
  }
}
