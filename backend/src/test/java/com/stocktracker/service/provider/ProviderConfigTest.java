package com.stocktracker.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProviderConfigTest {
  @Test
  void marketDataProviderSelectsYahooOnlyWhenConfigured() {
    var config = new ProviderConfig();
    config.marketDataProviderId = "yahoo";
    var stub = Mockito.mock(MarketDataProvider.class);
    var yahoo = Mockito.mock(MarketDataProvider.class);

    assertSame(yahoo, config.marketDataProvider(stub, yahoo));
    assertTrue(config.isLiveMarketDataProvider());
  }

  @Test
  void marketDataProviderDefaultsToStubAndNormalizesBlankId() {
    var config = new ProviderConfig();
    config.marketDataProviderId = " ";
    var stub = Mockito.mock(MarketDataProvider.class);
    var yahoo = Mockito.mock(MarketDataProvider.class);

    assertSame(stub, config.marketDataProvider(stub, yahoo));
    assertEquals("stub", config.marketDataProviderId());
    assertFalse(config.isLiveMarketDataProvider());
  }

  @Test
  void fxProviderSelectsFrankfurterWhenConfigured() {
    var config = new ProviderConfig();
    config.fxProviderId = "frankfurter";
    var stub = Mockito.mock(FxRateProvider.class);
    var frankfurter = Mockito.mock(FxRateProvider.class);

    assertSame(frankfurter, config.fxRateProvider(stub, frankfurter));
  }
}
