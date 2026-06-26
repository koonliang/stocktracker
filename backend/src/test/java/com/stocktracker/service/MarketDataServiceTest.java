package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.scheduler.FxRefreshJob;
import com.stocktracker.service.provider.MarketDataProvider;
import com.stocktracker.service.provider.ProviderConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class MarketDataServiceTest {
  private final MarketDataProvider marketDataProvider = Mockito.mock(MarketDataProvider.class);
  private final InstrumentRepository instrumentRepository = Mockito.mock(InstrumentRepository.class);
  private final QuoteRepository quoteRepository = Mockito.mock(QuoteRepository.class);
  private final QuoteCacheService quoteCacheService = Mockito.mock(QuoteCacheService.class);
  private final HistoricalBackfillService historicalBackfillService =
      Mockito.mock(HistoricalBackfillService.class);
  private final FxRefreshJob fxRefreshJob = Mockito.mock(FxRefreshJob.class);
  private final ProviderConfig providerConfig = Mockito.mock(ProviderConfig.class);

  private MarketDataService service;

  @BeforeEach
  void setUp() {
    service = Mockito.spy(new MarketDataService());
    service.marketDataProvider = marketDataProvider;
    service.instrumentRepository = instrumentRepository;
    service.quoteRepository = quoteRepository;
    service.quoteCacheService = quoteCacheService;
    service.historicalBackfillService = historicalBackfillService;
    service.fxRefreshJob = fxRefreshJob;
    service.providerConfig = providerConfig;
    service.clock = Clock.fixed(Instant.parse("2026-06-26T00:00:00Z"), ZoneOffset.UTC);
    service.self = service;
  }

  @Test
  void searchReturnsEmptyResultsForBlankQuery() {
    assertEquals(0, service.search(" ").results().size());
  }

  @Test
  void searchMapsProviderResults() {
    when(marketDataProvider.searchSymbols("aapl"))
        .thenReturn(List.of(new MarketDataProvider.ProviderSymbol("AAPL", "Apple", "NASDAQ", "USD")));

    var response = service.search("aapl");

    assertEquals(1, response.results().size());
    assertEquals("AAPL", response.results().getFirst().symbol());
    assertEquals("USD", response.results().getFirst().currency());
  }

  @Test
  void addInstrumentReturnsExistingInstrumentAndRefreshesQuote() {
    var instrument = instrument("AAPL", "Apple", "NASDAQ", "USD");
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("200");
    quote.asOf = Instant.parse("2026-06-26T00:00:00Z");
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument));
    when(quoteRepository.findBySymbol("AAPL")).thenReturn(Optional.of(quote));
    when(quoteCacheService.effectiveStale(quote)).thenReturn(false);

    var response = service.addInstrument(" aapl ");

    verify(quoteCacheService).refreshSymbols(List.of("AAPL"));
    assertEquals("AAPL", response.symbol());
    assertEquals(200.0, response.quote().price());
  }

  @Test
  void addInstrumentCreatesNewInstrumentAndBootstrapsHistoryAndFx() {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "SONY";
    quote.price = new BigDecimal("95.5");
    quote.asOf = Instant.parse("2026-06-26T00:00:00Z");
    when(instrumentRepository.findBySymbol("SONY"))
        .thenReturn(Optional.empty(), Optional.empty());
    when(marketDataProvider.searchSymbols("SONY"))
        .thenReturn(List.of(new MarketDataProvider.ProviderSymbol("SONY", "Sony", "NYSE", "JPY")));
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(true);
    when(quoteRepository.findBySymbol("SONY")).thenReturn(Optional.of(quote));
    when(quoteCacheService.effectiveStale(quote)).thenReturn(true);

    var response = service.addInstrument("sony");

    verify(instrumentRepository).persist(any(Instrument.class));
    verify(quoteCacheService).refreshSymbols(List.of("SONY"));
    verify(historicalBackfillService).backfillTrailingYear("SONY");
    verify(fxRefreshJob).refresh();
    assertEquals("JPY", response.currency());
    assertEquals(true, response.quote().stale());
  }

  @Test
  void addInstrumentRejectsUnknownSymbol() {
    when(instrumentRepository.findBySymbol("MISS")).thenReturn(Optional.empty());
    when(marketDataProvider.searchSymbols("MISS")).thenReturn(List.of());

    var error = assertThrows(ApiException.class, () -> service.addInstrument("miss"));

    assertEquals("unknown_symbol", error.code());
  }

  @Test
  void buildHistoryRefreshPlanVariesByProviderAndExistingBars() {
    when(instrumentRepository.listPriceBars("AAPL")).thenReturn(List.of());
    var liveEmpty = service.buildHistoryRefreshPlan("AAPL", LocalDate.parse("2026-06-26"), true);

    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(priceBar("2024-07-01", "100"), priceBar("2026-06-25", "110")));
    var liveRecentEnough = service.buildHistoryRefreshPlan("AAPL", LocalDate.parse("2026-06-26"), true);

    when(instrumentRepository.listPriceBars("AAPL")).thenReturn(List.of());
    var stubEmpty = service.buildHistoryRefreshPlan("AAPL", LocalDate.parse("2026-06-26"), false);

    assertEquals("TRAILING_YEAR", planAction(liveEmpty));
    assertEquals("MAX", planAction(liveRecentEnough));
    assertEquals(null, planFrom(liveRecentEnough));
    assertEquals(LocalDate.parse("2021-06-26"), planFrom(stubEmpty));
  }

  @Test
  void buildHistoryRefreshPlanUsesFromDateForLiveProviderWithDeepHistory() {
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(priceBar("2020-01-01", "90"), priceBar("2026-06-25", "110")));

    var plan = service.buildHistoryRefreshPlan("AAPL", LocalDate.parse("2026-06-26"), true);

    assertEquals("FROM_DATE", planAction(plan));
    assertEquals(LocalDate.parse("2026-06-25"), planFrom(plan));
  }

  @Test
  void buildHistoryRefreshPlanUsesExistingLatestDateForStubProvider() {
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(priceBar("2026-06-24", "100"), priceBar("2026-06-25", "110")));

    var plan = service.buildHistoryRefreshPlan("AAPL", LocalDate.parse("2026-06-26"), false);

    assertEquals("FROM_DATE", planAction(plan));
    assertEquals(LocalDate.parse("2026-06-25"), planFrom(plan));
  }

  @Test
  void bootstrapTrackedSymbolsAndAnalysisRefreshesQuotesHistoryAndSnapshot() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    doNothing().when(service).persistSnapshotArtifacts(eq("AAPL"), any());
    when(service.hasNoPriceBars("AAPL")).thenReturn(true);
    when(marketDataProvider.latestSnapshot("AAPL"))
        .thenReturn(
            new MarketDataProvider.ProviderSnapshot(
                "AAPL", null, null, null, null, null, null, null, null, null, null));

    service.bootstrapTrackedSymbolsAndAnalysis(List.of("aapl"));

    verify(quoteCacheService).refreshSymbols(List.of("AAPL"));
    verify(historicalBackfillService).backfillMax("AAPL");
    verify(service).persistSnapshotArtifacts(eq("AAPL"), any());
  }

  @Test
  void refreshTrackedSymbolsAndAnalysisRefreshesHistoryPlanAndSnapshot() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    doNothing().when(service).persistSnapshotArtifacts(eq("AAPL"), any());
    when(historicalBackfillService.backfill("AAPL", LocalDate.parse("2021-06-26"))).thenReturn(1);
    when(marketDataProvider.latestSnapshot("AAPL"))
        .thenReturn(
            new MarketDataProvider.ProviderSnapshot(
                "AAPL", null, null, null, null, null, null, null, null, null, null));

    service.refreshTrackedSymbolsAndAnalysis(List.of("aapl", "AAPL"));

    verify(historicalBackfillService).backfill("AAPL", LocalDate.parse("2021-06-26"));
    verify(service).persistSnapshotArtifacts(eq("AAPL"), any());
  }

  @Test
  void rewriteTrackedSymbolsAndAnalysisDeletesArtifactsBeforeRewriting() {
    doNothing().when(service).deleteTrackedAnalysisArtifacts(List.of("AAPL"));
    doNothing().when(service).persistSnapshotArtifacts(eq("AAPL"), any());
    when(marketDataProvider.latestSnapshot("AAPL"))
        .thenReturn(
            new MarketDataProvider.ProviderSnapshot(
                "AAPL", null, null, null, null, null, null, null, null, null, null));

    service.rewriteTrackedSymbolsAndAnalysis(List.of("aapl"));

    verify(service).deleteTrackedAnalysisArtifacts(List.of("AAPL"));
    verify(quoteCacheService).refreshSymbols(List.of("AAPL"));
    verify(historicalBackfillService).rewriteMax("AAPL");
    verify(service).persistSnapshotArtifacts(eq("AAPL"), any());
  }

  @Test
  void addInstrumentUsesEmptyQuoteSummaryWhenQuoteIsMissing() {
    when(instrumentRepository.findBySymbol("IBM")).thenReturn(Optional.of(instrument("IBM", "IBM", "NYSE", "USD")));
    when(quoteRepository.findBySymbol("IBM")).thenReturn(Optional.empty());

    var response = service.addInstrument("IBM");

    assertEquals("IBM", response.symbol());
    assertEquals(null, response.quote().price());
    assertEquals(true, response.quote().stale());
  }

  @Test
  void addInstrumentUsesStubHistoryPathAndDefaultsMissingExchangeAndCurrency() {
    when(instrumentRepository.findBySymbol("MYST"))
        .thenReturn(Optional.empty(), Optional.empty());
    when(marketDataProvider.searchSymbols("MYST"))
        .thenReturn(List.of(new MarketDataProvider.ProviderSymbol("MYST", "Mystery", null, null)));
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    when(quoteRepository.findBySymbol("MYST")).thenReturn(Optional.empty());

    var response = service.addInstrument("myst");

    verify(historicalBackfillService).backfillMax("MYST");
    assertEquals("", response.exchange());
    assertEquals("USD", response.currency());
  }

  @Test
  void refreshTrackedSymbolsAndAnalysisSkipsEmptyInput() {
    service.refreshTrackedSymbolsAndAnalysis(List.of());

    verify(historicalBackfillService, never()).backfill(any(), any());
    verify(service, never()).persistSnapshotArtifacts(any(), any());
  }

  @Test
  void bootstrapTrackedSymbolsAndAnalysisUsesTrailingYearForLiveProvider() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(true);
    doNothing().when(service).persistSnapshotArtifacts(eq("AAPL"), any());
    when(service.hasNoPriceBars("AAPL")).thenReturn(true);
    when(marketDataProvider.latestSnapshot("AAPL"))
        .thenReturn(
            new MarketDataProvider.ProviderSnapshot(
                "AAPL", null, null, null, null, null, null, null, null, null, null));

    service.bootstrapTrackedSymbolsAndAnalysis(List.of("AAPL"));

    verify(historicalBackfillService).backfillTrailingYear("AAPL");
    verify(historicalBackfillService, never()).backfillMax("AAPL");
  }

  @Test
  void bootstrapTrackedSymbolsAndAnalysisSkipsHistoryWhenBarsAlreadyExist() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    doNothing().when(service).persistSnapshotArtifacts(eq("AAPL"), any());
    when(service.hasNoPriceBars("AAPL")).thenReturn(false);
    when(marketDataProvider.latestSnapshot("AAPL"))
        .thenReturn(
            new MarketDataProvider.ProviderSnapshot(
                "AAPL", null, null, null, null, null, null, null, null, null, null));

    service.bootstrapTrackedSymbolsAndAnalysis(List.of("AAPL"));

    verify(historicalBackfillService, never()).backfillMax("AAPL");
    verify(historicalBackfillService, never()).backfillTrailingYear("AAPL");
  }

  @Test
  void persistSnapshotArtifactsCreatesPriceBarAndStatFromQuoteAndSnapshot() {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("205");
    quote.previousClose = new BigDecimal("200");
    quote.asOf = Instant.parse("2026-06-26T12:00:00Z");
    when(quoteRepository.findBySymbol("AAPL")).thenReturn(Optional.of(quote));
    when(instrumentRepository.findPriceBar("AAPL", LocalDate.parse("2026-06-26")))
        .thenReturn(Optional.empty());
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(priceBar("2026-06-25", "198")));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());

    try (MockedConstruction<InstrumentPriceBar> bars = Mockito.mockConstruction(InstrumentPriceBar.class);
        MockedConstruction<InstrumentStat> stats = Mockito.mockConstruction(InstrumentStat.class)) {
      service.persistSnapshotArtifacts(
          "AAPL",
          new MarketDataProvider.ProviderSnapshot(
              "AAPL",
              new BigDecimal("201"),
              new BigDecimal("206"),
              new BigDecimal("199"),
              new BigDecimal("200"),
              1234L,
              new BigDecimal("250"),
              new BigDecimal("150"),
              5000000L,
              new BigDecimal("30"),
              LocalDate.parse("2026-06-26")));

      var bar = bars.constructed().getFirst();
      var stat = stats.constructed().getFirst();
      assertEquals("AAPL", bar.instrumentSymbol);
      assertEquals(new BigDecimal("205"), bar.closePrice);
      assertEquals(new BigDecimal("201"), bar.openPrice);
      verify(bar).persist();
      assertEquals("AAPL", stat.instrumentSymbol);
      assertEquals(new BigDecimal("250"), stat.week52High);
      assertEquals(1234L, stat.volume);
      verify(stat).persist();
    }
  }

  @Test
  void persistSnapshotArtifactsSkipsPriceBarWhenQuoteIsMissingButStillPersistsStatFromSnapshot() {
    when(quoteRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
    when(instrumentRepository.listPriceBars("AAPL")).thenReturn(List.of());
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());

    try (MockedConstruction<InstrumentPriceBar> bars = Mockito.mockConstruction(InstrumentPriceBar.class);
        MockedConstruction<InstrumentStat> stats = Mockito.mockConstruction(InstrumentStat.class)) {
      service.persistSnapshotArtifacts(
          "AAPL",
          new MarketDataProvider.ProviderSnapshot(
              "AAPL",
              new BigDecimal("201"),
              new BigDecimal("206"),
              new BigDecimal("199"),
              new BigDecimal("200"),
              1234L,
              new BigDecimal("250"),
              new BigDecimal("150"),
              5000000L,
              new BigDecimal("30"),
              LocalDate.parse("2026-06-26")));

      assertEquals(0, bars.constructed().size());
      var stat = stats.constructed().getFirst();
      assertEquals(new BigDecimal("201"), stat.openPrice);
      assertEquals(new BigDecimal("200"), stat.previousClose);
      verify(stat).persist();
    }
  }

  @Test
  void persistSnapshotArtifactsUsesQuoteDateWhenSnapshotDateMissing() {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("205");
    quote.previousClose = new BigDecimal("200");
    quote.asOf = Instant.parse("2026-06-26T12:00:00Z");
    when(quoteRepository.findBySymbol("AAPL")).thenReturn(Optional.of(quote));
    when(instrumentRepository.findPriceBar("AAPL", LocalDate.parse("2026-06-26")))
        .thenReturn(Optional.empty());
    when(instrumentRepository.listPriceBars("AAPL")).thenReturn(List.of());
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());

    try (MockedConstruction<InstrumentPriceBar> bars = Mockito.mockConstruction(InstrumentPriceBar.class);
        MockedConstruction<InstrumentStat> stats = Mockito.mockConstruction(InstrumentStat.class)) {
      service.persistSnapshotArtifacts(
          "AAPL",
          new MarketDataProvider.ProviderSnapshot(
              "AAPL",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null));

      var bar = bars.constructed().getFirst();
      var stat = stats.constructed().getFirst();
      assertEquals(LocalDate.parse("2026-06-26"), bar.tradeDate);
      assertEquals(new BigDecimal("200"), bar.openPrice);
      assertEquals(new BigDecimal("205"), bar.highPrice);
      assertEquals(new BigDecimal("205"), bar.lowPrice);
      assertEquals(LocalDate.parse("2026-06-26"), stat.asOfDate);
      assertEquals(0L, stat.marketCap);
      assertNull(stat.peRatio);
    }
  }

  @Test
  void persistSnapshotArtifactsUsesLatestBarFallbacksWhenSnapshotAndQuoteFieldsMissing() {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("205");
    quote.previousClose = null;
    when(quoteRepository.findBySymbol("AAPL")).thenReturn(Optional.of(quote));
    when(instrumentRepository.findPriceBar("AAPL", LocalDate.parse("2026-06-26")))
        .thenReturn(Optional.empty());
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(
            List.of(
                detailedBar("2025-06-26", "150", "250", "150", "175", 100L),
                detailedBar("2026-06-25", "190", "210", "180", "200", 999L)));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());

    try (MockedConstruction<InstrumentPriceBar> bars = Mockito.mockConstruction(InstrumentPriceBar.class);
        MockedConstruction<InstrumentStat> stats = Mockito.mockConstruction(InstrumentStat.class)) {
      service.persistSnapshotArtifacts(
          "AAPL",
          new MarketDataProvider.ProviderSnapshot(
              "AAPL", null, null, null, null, null, null, null, null, null, null));

      var stat = stats.constructed().getFirst();
      assertEquals(new BigDecimal("190"), stat.openPrice);
      assertEquals(new BigDecimal("210"), stat.highPrice);
      assertEquals(new BigDecimal("180"), stat.lowPrice);
      assertEquals(new BigDecimal("200"), stat.previousClose);
      assertEquals(999L, stat.volume);
      assertEquals(new BigDecimal("250"), stat.week52High);
      assertEquals(new BigDecimal("150"), stat.week52Low);
    }
  }

  @Test
  void persistSnapshotArtifactsDoesNothingWhenQuoteAndSnapshotAndBarsAreAllMissing() {
    when(quoteRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
    when(instrumentRepository.listPriceBars("AAPL")).thenReturn(List.of());

    try (MockedConstruction<InstrumentPriceBar> bars = Mockito.mockConstruction(InstrumentPriceBar.class);
        MockedConstruction<InstrumentStat> stats = Mockito.mockConstruction(InstrumentStat.class)) {
      service.persistSnapshotArtifacts("AAPL", null);

      assertEquals(0, bars.constructed().size());
      assertEquals(0, stats.constructed().size());
    }
  }

  @Test
  void persistSnapshotArtifactsSkipsPersistForExistingManagedBarAndStat() {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("205");
    quote.previousClose = new BigDecimal("200");
    var existingBar = Mockito.mock(InstrumentPriceBar.class);
    var existingStat = Mockito.mock(InstrumentStat.class);
    when(quoteRepository.findBySymbol("AAPL")).thenReturn(Optional.of(quote));
    when(instrumentRepository.findPriceBar("AAPL", LocalDate.parse("2026-06-26")))
        .thenReturn(Optional.of(existingBar));
    when(instrumentRepository.listPriceBars("AAPL")).thenReturn(List.of(detailedBar("2026-06-25", "190", "210", "180", "200", 999L)));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.of(existingStat));
    when(existingBar.isPersistent()).thenReturn(true);
    when(existingStat.isPersistent()).thenReturn(true);

    service.persistSnapshotArtifacts(
        "AAPL",
        new MarketDataProvider.ProviderSnapshot(
            "AAPL",
            new BigDecimal("201"),
            new BigDecimal("206"),
            new BigDecimal("199"),
            new BigDecimal("200"),
            1234L,
            new BigDecimal("250"),
            new BigDecimal("150"),
            5000000L,
            new BigDecimal("30"),
            LocalDate.parse("2026-06-26")));

    verify(existingBar, never()).persist();
    verify(existingStat, never()).persist();
  }

  private Instrument instrument(String symbol, String name, String exchange, String currency) {
    var instrument = new Instrument();
    instrument.symbol = symbol;
    instrument.name = name;
    instrument.exchange = exchange;
    instrument.currency = currency;
    instrument.active = true;
    return instrument;
  }

  private InstrumentPriceBar priceBar(String date, String close) {
    var bar = new InstrumentPriceBar();
    bar.tradeDate = LocalDate.parse(date);
    bar.openPrice = new BigDecimal(close);
    bar.highPrice = new BigDecimal(close);
    bar.lowPrice = new BigDecimal(close);
    bar.closePrice = new BigDecimal(close);
    bar.volume = 10L;
    return bar;
  }

  private InstrumentPriceBar detailedBar(
      String date, String open, String high, String low, String close, long volume) {
    var bar = new InstrumentPriceBar();
    bar.tradeDate = LocalDate.parse(date);
    bar.openPrice = new BigDecimal(open);
    bar.highPrice = new BigDecimal(high);
    bar.lowPrice = new BigDecimal(low);
    bar.closePrice = new BigDecimal(close);
    bar.volume = volume;
    return bar;
  }

  private String planAction(Object plan) {
    try {
      var method = plan.getClass().getDeclaredMethod("action");
      method.setAccessible(true);
      return ((Enum<?>) method.invoke(plan)).name();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private LocalDate planFrom(Object plan) {
    try {
      var method = plan.getClass().getDeclaredMethod("from");
      method.setAccessible(true);
      return (LocalDate) method.invoke(plan);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }
}
