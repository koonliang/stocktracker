package com.stocktracker.service;

import com.stocktracker.api.ApiException;
import com.stocktracker.dto.InstrumentAnalysisResponse;
import com.stocktracker.persistence.InstrumentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

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
    var priceHistory =
        instrumentRepository.listPriceBars(ticker).stream()
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
        stats == null
            ? null
            : new InstrumentAnalysisResponse.StatsView(
                stats.openPrice.doubleValue(),
                stats.highPrice.doubleValue(),
                stats.lowPrice.doubleValue(),
                stats.previousClose.doubleValue(),
                stats.volume,
                stats.week52High.doubleValue(),
                stats.week52Low.doubleValue(),
                stats.marketCap,
                stats.peRatio == null ? null : stats.peRatio.doubleValue()),
        quote,
        priceHistory,
        position == null
            ? null
            : new InstrumentAnalysisResponse.PositionSummary(
                position.shares(),
                position.averageCost(),
                position.marketValue(),
                position.unrealizedPnL(),
                position.unrealizedPnLPct()));
  }
}
