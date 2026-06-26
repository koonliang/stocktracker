package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.dto.QuoteResponse;
import com.stocktracker.persistence.InstrumentRepository;
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
import org.mockito.Mockito;

class InstrumentServiceTest {
  private final InstrumentRepository instrumentRepository = Mockito.mock(InstrumentRepository.class);
  private final PortfolioService portfolioService = Mockito.mock(PortfolioService.class);
  private final QuoteCacheService quoteCacheService = Mockito.mock(QuoteCacheService.class);
  private final HistoricalBackfillService historicalBackfillService =
      Mockito.mock(HistoricalBackfillService.class);
  private final ProviderConfig providerConfig = Mockito.mock(ProviderConfig.class);

  private InstrumentService service;

  @BeforeEach
  void setUp() {
    service = new InstrumentService();
    service.instrumentRepository = instrumentRepository;
    service.portfolioService = portfolioService;
    service.quoteCacheService = quoteCacheService;
    service.historicalBackfillService = historicalBackfillService;
    service.providerConfig = providerConfig;
    service.clock = Clock.fixed(Instant.parse("2026-06-26T00:00:00Z"), ZoneOffset.UTC);
  }

  @Test
  void getAnalysisUsesInstrumentStatsWhenStatsAreAtLeastAsFreshAsLatestBar() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument()));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.of(stat()));
    when(quoteCacheService.readQuotes(List.of("AAPL")))
        .thenReturn(
            new QuoteResponse(
                List.of(
                    new QuoteResponse.QuoteView(
                        "AAPL",
                        201.5,
                        "USD",
                        1.2,
                        0.6,
                        200.3,
                        Instant.parse("2026-06-26T00:00:00Z"),
                        Instant.parse("2026-06-26T00:00:00Z"),
                        "yahoo",
                        false))));
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(
            List.of(
                bar("2025-06-20", "180", "181", "179", "180"),
                bar("2026-06-24", "190", "205", "188", "200"),
                bar("2026-06-25", "200", "206", "199", "201")));
    when(portfolioService.findPosition("AAPL"))
        .thenReturn(new PortfolioService.PositionSnapshot(2.0, 150.0, 403.0, 103.0, 0.34));

    var response = service.getAnalysis(" aapl ", "bad-range");

    verify(historicalBackfillService, never()).backfillMax(Mockito.anyString());
    assertEquals("AAPL", response.ticker().symbol());
    assertEquals(180.0, response.stats().open());
    assertEquals(2500000000L, response.stats().marketCap());
    assertEquals(2.0, response.positionSummary().shares());
    assertEquals(2, response.priceHistory().size());
  }

  @Test
  void getAnalysisBuildsStatsFromBarsWhenNoInstrumentStatExists() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument()));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());
    when(quoteCacheService.readQuotes(List.of("AAPL")))
        .thenReturn(
            new QuoteResponse(
                List.of(
                    new QuoteResponse.QuoteView(
                        "AAPL", 201.5, "USD", null, null, 198.0, null, null, "price-bar", true))));
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(
            List.of(
                bar("2026-06-20", "190", "193", "189", "192"),
                bar("2026-06-25", "195", "205", "194", "201")));
    when(portfolioService.findPosition("AAPL")).thenReturn(null);

    var response = service.getAnalysis("AAPL", "1W");

    assertEquals(195.0, response.stats().open());
    assertEquals(205.0, response.stats().week52High());
    assertEquals(189.0, response.stats().week52Low());
    assertEquals(198.0, response.stats().previousClose());
    assertNull(response.positionSummary());
  }

  @Test
  void getAnalysisBackfillsMaxForAllRangeWhenStubHistoryIsTooShort() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(false);
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument()));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());
    when(quoteCacheService.readQuotes(List.of("AAPL")))
        .thenReturn(new QuoteResponse(List.of(new QuoteResponse.QuoteView("AAPL", null, "USD", null, null, null, null, null, null, true))));
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(bar("2024-01-01", "100", "101", "99", "100")));
    when(portfolioService.findPosition("AAPL")).thenReturn(null);

    service.getAnalysis("AAPL", "ALL");

    verify(historicalBackfillService).backfillMax("AAPL");
  }

  @Test
  void getAnalysisBackfillsTrailingYearForLiveProviderWithoutBars() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(true);
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument()));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());
    when(quoteCacheService.readQuotes(List.of("AAPL")))
        .thenReturn(new QuoteResponse(List.of(new QuoteResponse.QuoteView("AAPL", 100.0, "USD", null, null, null, null, null, null, true))));
    when(instrumentRepository.listPriceBars("AAPL")).thenReturn(List.of());
    when(portfolioService.findPosition("AAPL")).thenReturn(null);

    service.getAnalysis("AAPL", "1Y");

    verify(historicalBackfillService).backfillTrailingYear("AAPL");
  }

  @Test
  void getAnalysisBackfillsFromLatestBarWhenLiveHistoryIsStale() {
    when(providerConfig.isLiveMarketDataProvider()).thenReturn(true);
    when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(instrument()));
    when(instrumentRepository.findStat("AAPL")).thenReturn(Optional.empty());
    when(quoteCacheService.readQuotes(List.of("AAPL")))
        .thenReturn(new QuoteResponse(List.of(new QuoteResponse.QuoteView("AAPL", 100.0, "USD", null, null, null, null, null, null, true))));
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(List.of(bar("2026-06-20", "190", "193", "189", "192")));
    when(portfolioService.findPosition("AAPL")).thenReturn(null);

    service.getAnalysis("AAPL", "1Y");

    verify(historicalBackfillService).backfill("AAPL", LocalDate.parse("2026-06-20"));
  }

  @Test
  void getAnalysisRejectsUnknownTicker() {
    when(instrumentRepository.findBySymbol("MISS")).thenReturn(Optional.empty());

    var error = assertThrows(ApiException.class, () -> service.getAnalysis("miss", "1Y"));

    assertEquals("not_found", error.code());
  }

  private Instrument instrument() {
    var instrument = new Instrument();
    instrument.symbol = "AAPL";
    instrument.name = "Apple";
    instrument.sector = "Technology";
    instrument.exchange = "NASDAQ";
    instrument.currency = "USD";
    return instrument;
  }

  private InstrumentStat stat() {
    var stat = new InstrumentStat();
    stat.asOfDate = LocalDate.parse("2026-06-25");
    stat.openPrice = new BigDecimal("180");
    stat.highPrice = new BigDecimal("205");
    stat.lowPrice = new BigDecimal("175");
    stat.previousClose = new BigDecimal("179.5");
    stat.volume = 123456L;
    stat.week52High = new BigDecimal("210");
    stat.week52Low = new BigDecimal("120");
    stat.marketCap = 2500000000L;
    stat.peRatio = new BigDecimal("32.1");
    return stat;
  }

  private InstrumentPriceBar bar(
      String date, String open, String high, String low, String close) {
    var bar = new InstrumentPriceBar();
    bar.tradeDate = LocalDate.parse(date);
    bar.openPrice = new BigDecimal(open);
    bar.highPrice = new BigDecimal(high);
    bar.lowPrice = new BigDecimal(low);
    bar.closePrice = new BigDecimal(close);
    bar.volume = 1000L;
    return bar;
  }
}
