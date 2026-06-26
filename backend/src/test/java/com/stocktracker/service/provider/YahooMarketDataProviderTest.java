package com.stocktracker.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class YahooMarketDataProviderTest {
  private final YahooApi api = Mockito.mock(YahooApi.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void normalizesSingleLetterClassShareSymbolsForYahoo() {
    assertEquals("BRK-B", YahooMarketDataProvider.yahooSymbol("BRK.B"));
    assertEquals("BF-B", YahooMarketDataProvider.yahooSymbol("bf.b"));
  }

  @Test
  void keepsExchangeSuffixSymbolsUnchanged() {
    assertEquals("ES3.SI", YahooMarketDataProvider.yahooSymbol("ES3.SI"));
    assertEquals("D05.SI", YahooMarketDataProvider.yahooSymbol("D05.SI"));
    assertEquals("VWRD.L", YahooMarketDataProvider.yahooSymbol("VWRD.L"));
    assertEquals("IWDA.L", YahooMarketDataProvider.yahooSymbol("IWDA.L"));
    assertEquals("VWRA.L", YahooMarketDataProvider.yahooSymbol("VWRA.L"));
  }

  @Test
  void yahooSymbolReturnsInputForBlankAndNull() {
    assertNull(YahooMarketDataProvider.yahooSymbol(null));
    assertEquals(" ", YahooMarketDataProvider.yahooSymbol(" "));
  }

  @Test
  void latestQuotesReturnsEmptyForEmptyInput() {
    var provider = new YahooMarketDataProvider();
    provider.api = api;

    assertTrue(provider.latestQuotes(List.of()).isEmpty());
  }

  @Test
  void latestQuotesSkipsSymbolsWhenChartPriceIsMissingOrFails() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chart("MISS", "1d", "1d"))
        .thenReturn(objectMapper.readTree("{\"chart\":{\"result\":[{\"meta\":{}}]}}"));
    when(api.chart("ERR", "1d", "1d")).thenThrow(new RuntimeException("boom"));

    var quotes = provider.latestQuotes(List.of("MISS", "ERR"));

    assertTrue(quotes.isEmpty());
  }

  @Test
  void latestQuotesReadsPriceAndPreviousCloseFromChartMeta() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chart("AAPL", "1d", "1d"))
        .thenReturn(
            objectMapper.readTree(
                """
                {"chart":{"result":[{"meta":{
                  "regularMarketPrice":201.5,
                  "chartPreviousClose":198.0,
                  "regularMarketTime":1736323200
                }}]}}
                """));

    var quotes = provider.latestQuotes(List.of("AAPL"));

    assertEquals(1, quotes.size());
    assertEquals("AAPL", quotes.getFirst().symbol());
    assertEquals(201.5, quotes.getFirst().price().doubleValue());
    assertEquals(198.0, quotes.getFirst().previousClose().doubleValue());
  }

  @Test
  void latestQuotesUsesNowWhenMarketTimeIsMissing() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chart("AAPL", "1d", "1d"))
        .thenReturn(
            objectMapper.readTree(
                """
                {"chart":{"result":[{"meta":{
                  "regularMarketPrice":201.5,
                  "chartPreviousClose":198.0
                }}]}}
                """));

    var quote = provider.latestQuotes(List.of("AAPL")).getFirst();

    assertEquals(201.5, quote.price().doubleValue());
    assertTrue(quote.asOf().isAfter(Instant.EPOCH));
  }

  @Test
  void latestSnapshotReturnsNullWhenChartMetaMissing() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chart("AAPL", "1d", "1d")).thenReturn(objectMapper.readTree("{\"chart\":{\"result\":[]}}"));

    assertNull(provider.latestSnapshot("AAPL"));
  }

  @Test
  void latestSnapshotUsesTodayWhenMarketTimeIsMissing() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    var today = LocalDate.now(ZoneOffset.UTC);
    when(api.chart("AAPL", "1d", "1d"))
        .thenReturn(
            objectMapper.readTree(
                """
                {"chart":{"result":[{"meta":{
                  "regularMarketOpen":200.0,
                  "regularMarketDayHigh":210.0,
                  "regularMarketDayLow":190.0,
                  "chartPreviousClose":198.0,
                  "regularMarketVolume":1234,
                  "fiftyTwoWeekHigh":250.0,
                  "fiftyTwoWeekLow":150.0,
                  "marketCap":5000000,
                  "trailingPE":30.0
                }}]}}
                """));

    var snapshot = provider.latestSnapshot("AAPL");

    assertEquals(today, snapshot.asOfDate());
  }

  @Test
  void latestSnapshotReturnsNullWhenApiThrows() {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chart("AAPL", "1d", "1d")).thenThrow(new RuntimeException("boom"));

    assertNull(provider.latestSnapshot("AAPL"));
  }

  @Test
  void dailyHistoryFiltersNullAndOutOfRangeBars() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chartPeriod(Mockito.eq("AAPL"), Mockito.eq("1d"), Mockito.anyLong(), Mockito.anyLong()))
        .thenReturn(
            objectMapper.readTree(
                """
                {"chart":{"result":[
                  {"timestamp":[1736121600,1736208000,1736294400],
                   "indicators":{"quote":[{"close":[100.5,null,102.25]}]}}
                ]}}
                """));

    var bars = provider.dailyHistory("AAPL", LocalDate.of(2025, 1, 5));

    assertEquals(2, bars.size());
    assertEquals(LocalDate.of(2025, 1, 6), bars.getFirst().date());
  }

  @Test
  void dailyHistoryReturnsEmptyOnBoundary400() {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chartPeriod(Mockito.eq("AAPL"), Mockito.eq("1d"), Mockito.anyLong(), Mockito.anyLong()))
        .thenThrow(new WebApplicationException(Response.status(400).build()));

    assertTrue(provider.dailyHistory("AAPL", LocalDate.of(2025, 1, 1)).isEmpty());
  }

  @Test
  void dailyHistoryReturnsEmptyOnNonBoundaryFailure() {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chartPeriod(Mockito.eq("AAPL"), Mockito.eq("1d"), Mockito.anyLong(), Mockito.anyLong()))
        .thenThrow(new WebApplicationException(Response.status(500).build()));

    assertTrue(provider.dailyHistory("AAPL", LocalDate.of(2025, 1, 1)).isEmpty());
  }

  @Test
  void dailyHistoryReturnsEmptyWhenApiThrowsRuntimeException() {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.chartPeriod(Mockito.eq("AAPL"), Mockito.eq("1d"), Mockito.anyLong(), Mockito.anyLong()))
        .thenThrow(new RuntimeException("boom"));

    assertTrue(provider.dailyHistory("AAPL", LocalDate.of(2025, 1, 1)).isEmpty());
  }

  @Test
  void searchSymbolsFiltersUnsupportedQuoteTypesAndUsesChartCurrency() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.search("apple"))
        .thenReturn(
            objectMapper.readTree(
                """
                {"quotes":[
                  {"symbol":"AAPL","quoteType":"EQUITY","longname":"Apple Inc","exchDisp":"NASDAQ"},
                  {"symbol":"BTC-USD","quoteType":"CRYPTO","shortname":"Bitcoin","exchange":"CCC"}
                ]}
                """));
    when(api.chart("AAPL", "1d", "1d"))
        .thenReturn(objectMapper.readTree("{\"chart\":{\"result\":[{\"meta\":{\"currency\":\"USD\"}}]}}"));

    var results = provider.searchSymbols("apple");

    verify(api).chart("AAPL", "1d", "1d");
    assertEquals(1, results.size());
    assertEquals("USD", results.getFirst().currency());
  }

  @Test
  void searchSymbolsSkipsMissingSymbolAndDefaultsCurrencyWhenMetaLookupFails() throws Exception {
    var provider = new YahooMarketDataProvider();
    provider.api = api;
    when(api.search("apple"))
        .thenReturn(
            objectMapper.readTree(
                """
                {"quotes":[
                  {"quoteType":"EQUITY","longname":"Ignored"},
                  {"symbol":"AAPL","quoteType":"EQUITY","shortname":"Apple"}
                ]}
                """));
    when(api.chart("AAPL", "1d", "1d")).thenThrow(new RuntimeException("boom"));

    var results = provider.searchSymbols("apple");

    assertEquals(1, results.size());
    assertEquals("AAPL", results.getFirst().symbol());
    assertEquals("USD", results.getFirst().currency());
    assertFalse(results.getFirst().name().isBlank());
  }
}
