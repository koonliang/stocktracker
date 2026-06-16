package com.stocktracker.service.provider;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Selects the active market-data and FX providers from config at runtime (one build serves dev +
 * prod — research §2). The unqualified {@link MarketDataProvider}/{@link FxRateProvider} beans
 * injected everywhere else resolve to these producers; the concrete impls stay {@code @Identifier}-
 * qualified so they never clash.
 */
@ApplicationScoped
public class ProviderConfig {
  @ConfigProperty(name = "stocktracker.marketdata.provider", defaultValue = "stub")
  String marketDataProviderId;

  @ConfigProperty(name = "stocktracker.fx.provider", defaultValue = "stub")
  String fxProviderId;

  @Produces
  @ApplicationScoped
  MarketDataProvider marketDataProvider(
      @Identifier("stub") MarketDataProvider stub, @Identifier("yahoo") MarketDataProvider yahoo) {
    return "yahoo".equalsIgnoreCase(marketDataProviderId) ? yahoo : stub;
  }

  @Produces
  @ApplicationScoped
  FxRateProvider fxRateProvider(
      @Identifier("stub") FxRateProvider stub,
      @Identifier("frankfurter") FxRateProvider frankfurter) {
    return "frankfurter".equalsIgnoreCase(fxProviderId) ? frankfurter : stub;
  }
}
