package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.scheduler.PriceHistoryRefreshJob;
import com.stocktracker.service.provider.MarketDataProvider;
import com.stocktracker.support.IntegrationTestSupport;
import com.stocktracker.support.MySqlTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MySqlTestResource.class)
@TestProfile(PriceHistoryRefreshJobLiveProviderTest.LiveProviderProfile.class)
class PriceHistoryRefreshJobLiveProviderTest extends IntegrationTestSupport {
  public static class LiveProviderProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("stocktracker.marketdata.provider", "yahoo");
    }
  }

  @Inject PriceHistoryRefreshJob priceHistoryRefreshJob;
  @Inject InstrumentRepository instrumentRepository;

  @BeforeEach
  void setUpProvider() throws Exception {
    QuarkusMock.installMockForType(
        new MarketDataProvider() {
          @Override
          public List<ProviderQuote> latestQuotes(Collection<String> symbols) {
            return symbols.stream()
                .map(
                    symbol ->
                        new ProviderQuote(
                            symbol,
                            BigDecimal.valueOf(105),
                            BigDecimal.valueOf(100),
                            Instant.parse("2026-06-22T00:00:00Z")))
                .toList();
          }

          @Override
          public List<ProviderDailyBar> dailyHistory(String symbol, LocalDate from) {
            return List.of(
                new ProviderDailyBar(symbol, LocalDate.parse("2026-06-21"), BigDecimal.valueOf(101)),
                new ProviderDailyBar(symbol, LocalDate.parse("2026-06-22"), BigDecimal.valueOf(104)));
          }

          @Override
          public List<ProviderDailyBar> dailyHistoryMax(String symbol) {
            return List.of();
          }

          @Override
          public List<ProviderSymbol> searchSymbols(String query) {
            return List.of();
          }

          @Override
          public ProviderSnapshot latestSnapshot(String symbol) {
            return new ProviderSnapshot(
                symbol,
                BigDecimal.valueOf(102),
                BigDecimal.valueOf(106),
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(100),
                12345L,
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(80),
                999999L,
                BigDecimal.valueOf(20),
                LocalDate.parse("2026-06-22"));
          }
        },
        MarketDataProvider.class);

    inTransaction(
        () -> {
          InstrumentPriceBar.delete("instrumentSymbol", "NVDA");
          com.stocktracker.domain.InstrumentQuote.deleteAll();
          com.stocktracker.domain.InstrumentStat.delete("instrumentSymbol", "NVDA");
        });
  }

  @Test
  void refreshAppendsMissingBarsAndUpdatesCurrentDaySnapshot() throws Exception {
    persistTransaction("2024-03-01", "NVDA", "buy", "5", "100.0000", "0.0000");
    inTransaction(
        () -> {
          var prior = new InstrumentPriceBar();
          prior.instrumentSymbol = "NVDA";
          prior.tradeDate = LocalDate.parse("2026-06-20");
          prior.openPrice = BigDecimal.valueOf(98);
          prior.highPrice = BigDecimal.valueOf(98);
          prior.lowPrice = BigDecimal.valueOf(98);
          prior.closePrice = BigDecimal.valueOf(98);
          prior.volume = 0L;
          prior.persist();
        });

    priceHistoryRefreshJob.refresh();

    var bars = instrumentRepository.listPriceBars("NVDA");
    assertEquals(3, bars.size());
    assertEquals(LocalDate.parse("2026-06-22"), bars.getLast().tradeDate);
    assertEquals(0, BigDecimal.valueOf(105).compareTo(bars.getLast().closePrice));
    assertEquals(12345L, bars.getLast().volume);
    var stat = instrumentRepository.findStat("NVDA").orElseThrow();
    assertEquals(0, BigDecimal.valueOf(150).compareTo(stat.week52High));
    assertTrue(instrumentRepository.findPriceBar("NVDA", LocalDate.parse("2026-06-21")).isPresent());
  }
}
