package com.stocktracker.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.persistence.InstrumentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StubMarketDataProviderTest {
  private final InstrumentRepository instruments = Mockito.mock(InstrumentRepository.class);
  private StubMarketDataProvider provider;

  @BeforeEach
  void setUp() {
    provider = new StubMarketDataProvider();
    provider.instruments = instruments;
    provider.objectMapper = new ObjectMapper();
    provider.clock = Clock.fixed(Instant.parse("2025-01-08T10:15:00Z"), ZoneOffset.UTC);
  }

  @Test
  void returnsFixtureQuoteForKnownSymbol() {
    var quotes = provider.latestQuotes(List.of("AAPL", "UNKNOWN"));

    assertEquals(1, quotes.size());
    assertEquals("AAPL", quotes.getFirst().symbol());
    assertTrue(quotes.getFirst().price().compareTo(BigDecimal.ZERO) > 0);
  }

  @Test
  void synthesizesWeekdayHistoryFromFixture() {
    var bars = provider.dailyHistory("AAPL", LocalDate.of(2025, 1, 3));

    assertFalse(bars.isEmpty());
    assertTrue(bars.stream().noneMatch(bar -> bar.date().getDayOfWeek().getValue() >= 6));
  }

  @Test
  void synthesizesHistoryForKnownPersistedSymbolWithoutBars() {
    when(instruments.listPriceBars("IBM")).thenReturn(List.of());
    when(instruments.existsSymbol("IBM")).thenReturn(true);

    var bars = provider.dailyHistory("IBM", LocalDate.of(2025, 1, 6));

    assertFalse(bars.isEmpty());
    assertEquals("IBM", bars.getFirst().symbol());
    assertTrue(bars.stream().allMatch(bar -> !bar.date().isBefore(LocalDate.of(2025, 1, 6))));
  }

  @Test
  void dailyHistoryMaxExtendsBackTenYears() {
    var bars = provider.dailyHistoryMax("AAPL");

    assertFalse(bars.isEmpty());
    assertTrue(bars.getFirst().date().isBefore(LocalDate.of(2016, 1, 10)));
  }

  @Test
  void usesRepositorySearchBeforeFixtureMatches() {
    var instrument = new Instrument();
    instrument.symbol = "MSFT";
    instrument.name = "Microsoft";
    instrument.exchange = "NASDAQ";
    instrument.currency = "USD";
    when(instruments.search("micro", 20)).thenReturn(List.of(instrument));

    var results = provider.searchSymbols("micro");

    assertEquals("MSFT", results.getFirst().symbol());
  }

  @Test
  void derivesSnapshotFromPersistedBarsWhenAvailable() {
    when(instruments.listPriceBars("AAPL"))
        .thenReturn(
            List.of(
                bar("2025-01-06", "100", "105", "99", "104"),
                bar("2025-01-07", "104", "106", "103", "105")));

    var snapshot = provider.latestSnapshot("AAPL");

    assertEquals(new BigDecimal("104"), snapshot.openPrice());
    assertEquals(new BigDecimal("106"), snapshot.highPrice());
    assertEquals(new BigDecimal("103"), snapshot.lowPrice());
    assertEquals(new BigDecimal("104"), snapshot.previousClose());
  }

  @Test
  void derivesSnapshotFromFixtureWhenNoBarsExist() {
    when(instruments.listPriceBars("AAPL")).thenReturn(List.of());

    var snapshot = provider.latestSnapshot("AAPL");

    assertNotNull(snapshot);
    assertEquals("AAPL", snapshot.symbol());
    assertEquals(LocalDate.of(2025, 1, 8), snapshot.asOfDate());
    assertEquals(snapshot.highPrice(), snapshot.lowPrice());
  }

  @Test
  void returnsNullSnapshotForUnknownSymbol() {
    when(instruments.listPriceBars("UNKNOWN")).thenReturn(List.of());

    assertNull(provider.latestSnapshot("UNKNOWN"));
  }

  @Test
  void blankSearchReturnsEmptyList() {
    assertTrue(provider.searchSymbols(" ").isEmpty());
  }

  private InstrumentPriceBar bar(String date, String open, String high, String low, String close) {
    var bar = new InstrumentPriceBar();
    bar.tradeDate = LocalDate.parse(date);
    bar.openPrice = new BigDecimal(open);
    bar.highPrice = new BigDecimal(high);
    bar.lowPrice = new BigDecimal(low);
    bar.closePrice = new BigDecimal(close);
    return bar;
  }
}
