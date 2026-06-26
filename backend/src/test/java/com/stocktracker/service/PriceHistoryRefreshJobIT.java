package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.scheduler.PriceHistoryRefreshJob;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
class PriceHistoryRefreshJobIT extends IntegrationTestSupport {
  @Inject PriceHistoryRefreshJob priceHistoryRefreshJob;
  @Inject InstrumentRepository instrumentRepository;

  @Test
  void refreshBackfillsBarsAndStatsForTrackedSymbols() throws Exception {
    persistTransaction("2024-03-01", "AAPL", "buy", "5", "100.0000", "0.0000");

    priceHistoryRefreshJob.refresh();

    var bars = instrumentRepository.listPriceBars("AAPL");
    assertFalse(bars.isEmpty());

    var stat = instrumentRepository.findStat("AAPL").orElse(null);
    assertNotNull(stat);
    assertNotNull(stat.asOfDate);
    assertNotNull(stat.previousClose);
  }
}
