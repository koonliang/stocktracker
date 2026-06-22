package com.stocktracker.service.provider;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Internal seam isolating the external quote source (contracts/market-data-provider.md). Selected
 * at runtime by {@code stocktracker.marketdata.provider}: {@code stub} (default — dev + all tests)
 * or {@code yahoo} (prod). Implementations never throw into a request path; transient failures are
 * absorbed by {@code QuoteRefreshJob} (FR-006).
 */
public interface MarketDataProvider {
  /** Latest quote per symbol, batched. Missing/unknown symbols are omitted. */
  List<ProviderQuote> latestQuotes(Collection<String> symbols);

  /**
   * Latest analysis snapshot for a symbol. Providers may omit unsupported fields by returning
   * {@code null} values or {@code null} overall.
   */
  default ProviderSnapshot latestSnapshot(String symbol) {
    return null;
  }

  /** Daily closing prices for a symbol from {@code from} (inclusive) to today. */
  List<ProviderDailyBar> dailyHistory(String symbol, LocalDate from);

  /** Maximum available daily closing prices for a symbol. */
  List<ProviderDailyBar> dailyHistoryMax(String symbol);

  /** Search symbols by company name or partial/exact ticker (FR-026). */
  List<ProviderSymbol> searchSymbols(String query);

  record ProviderQuote(String symbol, BigDecimal price, BigDecimal previousClose, Instant asOf) {}

  record ProviderDailyBar(String symbol, LocalDate date, BigDecimal close) {}

  record ProviderSymbol(String symbol, String name, String exchange, String currency) {}

  record ProviderSnapshot(
      String symbol,
      BigDecimal openPrice,
      BigDecimal highPrice,
      BigDecimal lowPrice,
      BigDecimal previousClose,
      Long volume,
      BigDecimal week52High,
      BigDecimal week52Low,
      Long marketCap,
      BigDecimal peRatio,
      LocalDate asOfDate) {}
}
