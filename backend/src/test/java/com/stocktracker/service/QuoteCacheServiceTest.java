package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stocktracker.domain.Instrument;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentQuote;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.persistence.QuoteRepository;
import com.stocktracker.service.provider.MarketDataProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QuoteCacheServiceTest {
  private final MarketDataProvider marketDataProvider = Mockito.mock(MarketDataProvider.class);
  private final QuoteRepository quoteRepository = Mockito.mock(QuoteRepository.class);
  private final InstrumentRepository instrumentRepository = Mockito.mock(InstrumentRepository.class);
  private final AlertEvaluationService alertEvaluationService =
      Mockito.mock(AlertEvaluationService.class);

  private QuoteCacheService service;

  @BeforeEach
  void setUp() {
    service = Mockito.spy(new QuoteCacheService());
    service.marketDataProvider = marketDataProvider;
    service.quoteRepository = quoteRepository;
    service.instrumentRepository = instrumentRepository;
    service.alertEvaluationService = alertEvaluationService;
    service.clock = Clock.fixed(Instant.parse("2026-06-26T09:00:00Z"), ZoneOffset.UTC);
    service.refreshInterval = Duration.ofSeconds(60);
    service.staleAfterIntervals = 3;
    service.providerId = "yahoo";
    service.self = service;
  }

  @Test
  void refreshSymbolsUppercasesDeduplicatesAndPersistsFetchedQuotes() {
    var existing = new InstrumentQuote();
    existing.instrumentSymbol = "AAPL";
    when(marketDataProvider.latestQuotes(List.of("AAPL")))
        .thenReturn(
            List.of(
                new MarketDataProvider.ProviderQuote(
                    "aapl",
                    new BigDecimal("125.50"),
                    new BigDecimal("120.50"),
                    Instant.parse("2026-06-26T08:59:00Z"))));
    when(quoteRepository.findOrNew("AAPL")).thenReturn(existing);

    service.refreshSymbols(List.of("aapl", "AAPL"));

    assertEquals(new BigDecimal("125.50"), existing.price);
    assertEquals(new BigDecimal("120.50"), existing.previousClose);
    assertEquals(new BigDecimal("5.00"), existing.changeAmount);
    assertEquals(new BigDecimal("4.1494"), existing.changePct);
    assertEquals("yahoo", existing.source);
    assertEquals(Instant.parse("2026-06-26T09:00:00Z"), existing.fetchedAt);
    verify(quoteRepository).persist(existing);
    verify(alertEvaluationService).evaluate(existing);
  }

  @Test
  void readQuotesRefreshesKnownMissingSymbolsAndReturnsCachedViews() {
    var quote = new InstrumentQuote();
    quote.instrumentSymbol = "AAPL";
    quote.price = new BigDecimal("123.45");
    quote.previousClose = new BigDecimal("120.00");
    quote.asOf = Instant.parse("2026-06-26T08:58:00Z");
    quote.fetchedAt = Instant.parse("2026-06-26T08:59:30Z");
    quote.source = "yahoo";

    var instrument = new Instrument();
    instrument.symbol = "AAPL";
    instrument.currency = "USD";

    Mockito.doReturn(List.of("AAPL")).when(service).findKnownStaleOrMissing(List.of("AAPL", "MSFT"));
    Mockito.doNothing().when(service).refreshSymbols(List.of("AAPL"));
    Mockito.doReturn(new com.stocktracker.dto.QuoteResponse(List.of()))
        .when(service)
        .readCachedQuotes(List.of());
    when(quoteRepository.findBySymbols(List.of("AAPL", "MSFT"))).thenReturn(List.of(quote));
    when(instrumentRepository.findBySymbols(List.of("AAPL", "MSFT")))
        .thenReturn(Map.of("AAPL", instrument));

    var response = service.readQuotes(List.of("aapl", "msft"));

    verify(service).refreshSymbols(List.of("AAPL"));
    assertEquals(2, response.quotes().size());
    assertEquals("AAPL", response.quotes().get(0).symbol());
    assertEquals(123.45, response.quotes().get(0).price());
    assertEquals("USD", response.quotes().get(0).currency());
    assertTrue(response.quotes().get(1).stale());
    assertNull(response.quotes().get(1).price());
  }

  @Test
  void readCachedQuotesFallsBackToLatestPriceBarWhenLiveQuoteMissing() {
    var instrument = new Instrument();
    instrument.symbol = "AAPL";
    instrument.currency = "USD";
    when(quoteRepository.findBySymbols(List.of("AAPL"))).thenReturn(List.of());
    when(instrumentRepository.findBySymbols(List.of("AAPL"))).thenReturn(Map.of("AAPL", instrument));
    when(instrumentRepository.listPriceBars("AAPL"))
        .thenReturn(
            List.of(
                priceBar("AAPL", "2026-06-24", "118"),
                priceBar("AAPL", "2026-06-25", "121.50")));

    var response = service.readCachedQuotes(List.of("AAPL"));

    assertEquals(1, response.quotes().size());
    var quote = response.quotes().getFirst();
    assertEquals(121.5, quote.price());
    assertEquals("price-bar", quote.source());
    assertTrue(quote.stale());
  }

  @Test
  void effectiveStaleTreatsMissingOrExpiredFetchAsStale() {
    var neverFetched = new InstrumentQuote();
    neverFetched.fetchedAt = null;

    var expired = new InstrumentQuote();
    expired.fetchedAt = Instant.parse("2026-06-26T08:55:59Z");

    var fresh = new InstrumentQuote();
    fresh.fetchedAt = Instant.parse("2026-06-26T08:57:30Z");

    assertTrue(service.effectiveStale(neverFetched));
    assertTrue(service.effectiveStale(expired));
    assertEquals(false, service.effectiveStale(fresh));
  }

  @Test
  void refreshSymbolsIgnoresEmptyInputAndMissingFetches() {
    service.refreshSymbols(List.of());
    verify(marketDataProvider, never()).latestQuotes(java.util.List.of());

    var existing = new InstrumentQuote();
    existing.instrumentSymbol = "AAPL";
    when(marketDataProvider.latestQuotes(List.of("AAPL", "MSFT")))
        .thenReturn(List.of(new MarketDataProvider.ProviderQuote("AAPL", new BigDecimal("10"), new BigDecimal("9"), Instant.now())));
    when(quoteRepository.findOrNew("AAPL")).thenReturn(existing);

    service.refreshSymbols(List.of("AAPL", "MSFT"));

    verify(quoteRepository, never()).findOrNew("MSFT");
  }

  @Test
  void readCachedQuotesReturnsUnknownInstrumentAsNullPrice() {
    when(quoteRepository.findBySymbols(List.of("MISS"))).thenReturn(List.of());
    when(instrumentRepository.findBySymbols(List.of("MISS"))).thenReturn(Map.of());

    var response = service.readCachedQuotes(List.of("MISS"));

    assertNull(response.quotes().getFirst().price());
    assertTrue(response.quotes().getFirst().stale());
  }

  private InstrumentPriceBar priceBar(String symbol, String date, String close) {
    var bar = new InstrumentPriceBar();
    bar.instrumentSymbol = symbol;
    bar.tradeDate = LocalDate.parse(date);
    bar.closePrice = new BigDecimal(close);
    return bar;
  }
}
