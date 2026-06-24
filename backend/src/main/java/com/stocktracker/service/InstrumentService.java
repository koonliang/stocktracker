package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.dto.InstrumentAnalysisResponse;
import com.stocktracker.dto.QuoteResponse;
import com.stocktracker.persistence.InstrumentRepository;
import com.stocktracker.service.provider.ProviderConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class InstrumentService {
  @Inject InstrumentRepository instrumentRepository;
  @Inject PortfolioService portfolioService;
  @Inject QuoteCacheService quoteCacheService;
  @Inject HistoricalBackfillService historicalBackfillService;
  @Inject ProviderConfig providerConfig;
  @Inject Clock clock;

  public InstrumentAnalysisResponse getAnalysis(String rawTicker) {
    return getAnalysis(rawTicker, "1Y");
  }

  public InstrumentAnalysisResponse getAnalysis(String rawTicker, String range) {
    var ticker = rawTicker.trim().toUpperCase();
    var instrument =
        instrumentRepository
            .findBySymbol(ticker)
            .orElseThrow(() -> new ApiException(Status.NOT_FOUND, "not_found", "Ticker not found"));
    var normalizedRange = normalizeRange(range);
    ensureHistory(ticker, normalizedRange);
    var stats = instrumentRepository.findStat(ticker).orElse(null);
    // Live quote from the cache so the detail page matches the dashboard (not the stale last bar).
    var quote =
        quoteCacheService.readQuotes(java.util.List.of(ticker)).quotes().stream()
            .findFirst()
            .orElse(null);
    var allPriceHistory = instrumentRepository.listPriceBars(ticker);
    var priceHistory = filterBars(allPriceHistory, normalizedRange);
    var priceHistoryResponse =
        priceHistory.stream()
            .map(
                bar ->
                    new InstrumentAnalysisResponse.PriceHistoryPoint(
                        bar.tradeDate.toString(),
                        bar.openPrice.doubleValue(),
                        bar.highPrice.doubleValue(),
                        bar.lowPrice.doubleValue(),
                        bar.closePrice.doubleValue(),
                        bar.volume))
            .toList();
    var position = portfolioService.findPosition(ticker);
    return new InstrumentAnalysisResponse(
        new InstrumentAnalysisResponse.TickerView(
            instrument.symbol,
            instrument.name,
            instrument.sector,
            instrument.exchange,
            instrument.currency),
        statsView(stats, quote, allPriceHistory),
        quote,
        priceHistoryResponse,
        position == null
            ? null
            : new InstrumentAnalysisResponse.PositionSummary(
                position.shares(),
                position.averageCost(),
                position.marketValue(),
                position.unrealizedPnL(),
                position.unrealizedPnLPct()));
  }

  private InstrumentAnalysisResponse.StatsView statsView(
      InstrumentStat stats, QuoteResponse.QuoteView quote, List<InstrumentPriceBar> bars) {
    var latestBar = bars.isEmpty() ? null : bars.get(bars.size() - 1);
    if (stats != null && (latestBar == null || !stats.asOfDate.isBefore(latestBar.tradeDate))) {
      return new InstrumentAnalysisResponse.StatsView(
          dbl(stats.openPrice),
          dbl(stats.highPrice),
          dbl(stats.lowPrice),
          dbl(stats.previousClose),
          stats.volume,
          dbl(stats.week52High),
          dbl(stats.week52Low),
          stats.marketCap,
          dbl(stats.peRatio));
    }
    if (bars.isEmpty() && quote == null) {
      return null;
    }

    var previousBar = bars.size() < 2 ? null : bars.get(bars.size() - 2);
    var previousClose =
        quote != null && quote.previousClose() != null
            ? quote.previousClose()
            : previousBar == null ? null : dbl(previousBar.closePrice);
    return new InstrumentAnalysisResponse.StatsView(
        latestBar == null ? null : dbl(latestBar.openPrice),
        latestBar == null ? null : dbl(latestBar.highPrice),
        latestBar == null ? null : dbl(latestBar.lowPrice),
        previousClose,
        latestBar == null ? null : latestBar.volume,
        bars.stream()
            .map(bar -> bar.highPrice)
            .max(Comparator.naturalOrder())
            .map(this::dbl)
            .orElse(null),
        bars.stream()
            .map(bar -> bar.lowPrice)
            .min(Comparator.naturalOrder())
            .map(this::dbl)
            .orElse(null),
        null,
        null);
  }

  private Double dbl(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private void ensureHistory(String symbol, String range) {
    var today = LocalDate.now(clock);
    var bars = instrumentRepository.listPriceBars(symbol);
    if (providerConfig.isLiveMarketDataProvider()) {
      if (bars.isEmpty()) {
        historicalBackfillService.backfillTrailingYear(symbol);
        return;
      }
      if (bars.get(bars.size() - 1).tradeDate.isBefore(today.minusDays(1))) {
        historicalBackfillService.backfill(symbol, bars.get(bars.size() - 1).tradeDate);
      }
      return;
    }
    if ("ALL".equals(range)) {
      if (bars.isEmpty() || bars.get(0).tradeDate.isAfter(today.minusYears(5))) {
        historicalBackfillService.backfillMax(symbol);
      } else if (bars.get(bars.size() - 1).tradeDate.isBefore(today.minusDays(1))) {
        historicalBackfillService.backfill(symbol, bars.get(bars.size() - 1).tradeDate);
      }
      return;
    }

    var start = historyStart(range, today);
    if (bars.isEmpty()
        || bars.get(0).tradeDate.isAfter(start)
        || bars.get(bars.size() - 1).tradeDate.isBefore(today.minusDays(1))) {
      historicalBackfillService.backfill(symbol, start);
    }
  }

  private List<InstrumentPriceBar> filterBars(List<InstrumentPriceBar> bars, String range) {
    if ("ALL".equals(range)) {
      return bars;
    }
    var start = historyStart(range, LocalDate.now(clock));
    return bars.stream().filter(bar -> !bar.tradeDate.isBefore(start)).toList();
  }

  private String normalizeRange(String range) {
    var value = range == null || range.isBlank() ? "1Y" : range.toUpperCase(Locale.ROOT);
    return switch (value) {
      case "1D", "1W", "1M", "3M", "1Y", "5Y", "ALL" -> value;
      default -> "1Y";
    };
  }

  private LocalDate historyStart(String range, LocalDate today) {
    return switch (range) {
      case "1D" -> today.minusDays(1);
      case "1W" -> today.minusWeeks(1);
      case "1M" -> today.minusMonths(1);
      case "3M" -> today.minusMonths(3);
      case "5Y" -> today.minusYears(5);
      default -> today.minusYears(1);
    };
  }
}
