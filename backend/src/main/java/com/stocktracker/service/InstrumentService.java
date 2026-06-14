package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.domain.InstrumentPriceBar;
import com.stocktracker.domain.InstrumentStat;
import com.stocktracker.dto.InstrumentAnalysisResponse;
import com.stocktracker.dto.QuoteResponse;
import com.stocktracker.persistence.InstrumentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class InstrumentService {
  @Inject InstrumentRepository instrumentRepository;
  @Inject PortfolioService portfolioService;
  @Inject QuoteCacheService quoteCacheService;

  public InstrumentAnalysisResponse getAnalysis(String rawTicker) {
    var ticker = rawTicker.trim().toUpperCase();
    var instrument =
        instrumentRepository
            .findBySymbol(ticker)
            .orElseThrow(() -> new ApiException(Status.NOT_FOUND, "not_found", "Ticker not found"));
    var stats = instrumentRepository.findStat(ticker).orElse(null);
    // Live quote from the cache so the detail page matches the dashboard (not the stale last bar).
    var quote =
        quoteCacheService.readQuotes(java.util.List.of(ticker)).quotes().stream()
            .findFirst()
            .orElse(null);
    var priceHistory = instrumentRepository.listPriceBars(ticker);
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
            instrument.symbol, instrument.name, instrument.sector, instrument.exchange),
        statsView(stats, quote, priceHistory),
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
    if (stats != null) {
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

    var latestBar = bars.isEmpty() ? null : bars.get(bars.size() - 1);
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
}
