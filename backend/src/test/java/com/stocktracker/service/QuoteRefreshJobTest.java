package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.scheduler.QuoteRefreshJob;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Clock;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class QuoteRefreshJobTest extends IntegrationTestSupport {
  @Inject QuoteRefreshJob quoteRefreshJob;
  @Inject QuoteCacheService quoteCacheService;
  @Inject QuoteRepository quoteRepository;
  @Inject Clock clock;

  @Test
  void refreshUpsertsCacheForTrackedSymbols() throws Exception {
    persistTransaction("2024-03-01", "NVDA", "buy", "5", "100.0000", "0.0000");

    assertTrue(quoteRefreshJob.trackedSymbols().contains("NVDA"));

    quoteRefreshJob.refresh();

    var quote = quoteRepository.findBySymbol("NVDA").orElseThrow();
    assertNotNull(quote.price);
    assertNotNull(quote.fetchedAt);
    assertFalse(quote.stale);
    assertFalse(quoteCacheService.effectiveStale(quote));
  }

  @Test
  void effectiveStaleFlipsWhenFetchedAtAgesOut() {
    var fresh = new InstrumentQuote();
    fresh.fetchedAt = clock.instant();
    assertFalse(quoteCacheService.effectiveStale(fresh));

    var aged = new InstrumentQuote();
    aged.fetchedAt = clock.instant().minusSeconds(600); // older than 3 * 60s
    assertTrue(quoteCacheService.effectiveStale(aged));

    var neverFetched = new InstrumentQuote();
    assertTrue(quoteCacheService.effectiveStale(neverFetched));
  }
}
