package com.stocktracker.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class YahooMarketDataProviderTest {
  @Test
  void normalizesSingleLetterClassShareSymbolsForYahoo() {
    assertEquals("BRK-B", YahooMarketDataProvider.yahooSymbol("BRK.B"));
    assertEquals("BF-B", YahooMarketDataProvider.yahooSymbol("bf.b"));
  }

  @Test
  void keepsExchangeSuffixSymbolsUnchanged() {
    assertEquals("ES3.SI", YahooMarketDataProvider.yahooSymbol("ES3.SI"));
    assertEquals("D05.SI", YahooMarketDataProvider.yahooSymbol("D05.SI"));
  }
}
