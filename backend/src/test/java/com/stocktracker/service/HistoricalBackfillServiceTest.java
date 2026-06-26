package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.service.provider.MarketDataProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class HistoricalBackfillServiceTest {
  private final MarketDataProvider marketDataProvider = Mockito.mock(MarketDataProvider.class);
  private final InstrumentRepository instrumentRepository = Mockito.mock(InstrumentRepository.class);
  private HistoricalBackfillService service;

  @BeforeEach
  void setUp() {
    service = Mockito.spy(new HistoricalBackfillService());
    service.marketDataProvider = marketDataProvider;
    service.instrumentRepository = instrumentRepository;
    service.clock = Clock.fixed(Instant.parse("2026-06-26T00:00:00Z"), ZoneOffset.UTC);
    service.self = service;
  }

  @Test
  void backfillDelegatesToProviderHistoryAndInsertBars() {
    var bars = List.of(new MarketDataProvider.ProviderDailyBar("AAPL", LocalDate.parse("2026-06-25"), new BigDecimal("10")));
    when(marketDataProvider.dailyHistory("AAPL", LocalDate.parse("2026-06-01"))).thenReturn(bars);
    doReturn(1).when(service).insertBars("AAPL", bars);

    var inserted = service.backfill("AAPL", LocalDate.parse("2026-06-01"));

    assertEquals(1, inserted);
  }

  @Test
  void backfillTrailingYearUsesClockDerivedStart() {
    doReturn(2).when(service).backfill("AAPL", LocalDate.parse("2025-06-26"));

    assertEquals(2, service.backfillTrailingYear("AAPL"));
  }

  @Test
  void rewriteMaxDelegatesToProviderAndRewriteBars() {
    var bars =
        List.of(
            new MarketDataProvider.ProviderDailyBar(
                "AAPL", LocalDate.parse("2026-06-25"), new BigDecimal("10")));
    when(marketDataProvider.dailyHistoryMax("AAPL")).thenReturn(bars);
    doReturn(3).when(service).rewriteBars("AAPL", bars);

    var inserted = service.rewriteMax("AAPL");

    assertEquals(3, inserted);
  }

  @Test
  void insertBarsSkipsNullAndDuplicateDatesAndPersistsNewBars() {
    when(instrumentRepository.listPriceBars("aapl"))
        .thenReturn(List.of(existingBar("2026-06-24", "9")));
    var providerBars =
        List.of(
            new MarketDataProvider.ProviderDailyBar("AAPL", LocalDate.parse("2026-06-24"), new BigDecimal("9")),
            new MarketDataProvider.ProviderDailyBar("AAPL", LocalDate.parse("2026-06-25"), null),
            new MarketDataProvider.ProviderDailyBar("AAPL", LocalDate.parse("2026-06-26"), new BigDecimal("11")));

    try (MockedConstruction<InstrumentPriceBar> bars = Mockito.mockConstruction(InstrumentPriceBar.class)) {
      var inserted = service.insertBars("aapl", providerBars);

      assertEquals(1, inserted);
      var created = bars.constructed().getFirst();
      assertEquals("AAPL", created.instrumentSymbol);
      assertEquals(LocalDate.parse("2026-06-26"), created.tradeDate);
      assertEquals(new BigDecimal("11"), created.closePrice);
      verify(created).persist();
    }
  }

  private InstrumentPriceBar existingBar(String date, String close) {
    var bar = new InstrumentPriceBar();
    bar.tradeDate = LocalDate.parse(date);
    bar.closePrice = new BigDecimal(close);
    return bar;
  }
}
