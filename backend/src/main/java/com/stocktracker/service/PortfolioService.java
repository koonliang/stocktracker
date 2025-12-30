package com.stocktracker.service;

import com.stocktracker.client.YahooFinanceClient;
import com.stocktracker.client.dto.HistoricalData;
import com.stocktracker.client.dto.HistoricalPrice;
import com.stocktracker.client.dto.StockQuote;
import com.stocktracker.dto.response.HoldingResponse;
import com.stocktracker.dto.response.PortfolioPerformancePoint;
import com.stocktracker.dto.response.PortfolioResponse;
import com.stocktracker.entity.Holding;
import com.stocktracker.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final YahooFinanceClient yahooFinanceClient;

    /**
     * Get user's portfolio with live prices and calculated returns.
     * Results are cached for 2 minutes to reduce Yahoo Finance API calls.
     */
    @Cacheable(value = "portfolio", key = "#userId", unless = "#result == null")
    public PortfolioResponse getPortfolio(Long userId) {
        log.debug("Fetching portfolio for user: {}", userId);

        // 1. Fetch holdings from database
        List<Holding> holdings = holdingRepository.findByUserIdOrderBySymbolAsc(userId);

        if (holdings.isEmpty()) {
            log.debug("No holdings found for user: {}", userId);
            return PortfolioResponse.builder()
                    .holdings(Collections.emptyList())
                    .totalValue(BigDecimal.ZERO)
                    .totalCost(BigDecimal.ZERO)
                    .totalReturnDollars(BigDecimal.ZERO)
                    .totalReturnPercent(BigDecimal.ZERO)
                    .pricesUpdatedAt(LocalDateTime.now())
                    .build();
        }

        // 2. Fetch live prices from Yahoo Finance
        List<String> symbols = holdings.stream()
                .map(Holding::getSymbol)
                .collect(Collectors.toList());

        log.debug("Fetching live prices for {} symbols", symbols.size());
        Map<String, StockQuote> quotes = yahooFinanceClient.getQuotes(symbols);

        // 3. Fetch 7-day historical data for all symbols
        log.debug("Fetching 7-day historical data for {} symbols", symbols.size());
        Map<String, HistoricalData> historicalData7d = yahooFinanceClient.getHistoricalDataBatch(symbols, "7d");

        // 4. Fetch 1-year historical data for sparklines
        log.debug("Fetching 1-year historical data for {} symbols", symbols.size());
        Map<String, HistoricalData> historicalData1y = yahooFinanceClient.getHistoricalDataBatch(symbols, "1y");

        // 5. Build holding responses with new fields
        List<HoldingResponse> holdingResponses = holdings.stream()
                .map(h -> {
                    HoldingResponse response = buildHoldingResponse(h, quotes.get(h.getSymbol()));

                    // Add 7D return
                    calculate7DayReturn(response, historicalData7d.get(h.getSymbol()));

                    // Add sparkline data
                    response.setSparklineData(extractSparklineData(historicalData1y.get(h.getSymbol())));

                    return response;
                })
                .collect(Collectors.toList());

        // 6. Calculate total portfolio value first
        BigDecimal totalValue = holdingResponses.stream()
                .map(HoldingResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7. Add weight to each holding
        holdingResponses.forEach(h -> calculateWeight(h, totalValue));

        // 8. Build and return portfolio response
        PortfolioResponse response = buildPortfolioResponse(holdingResponses);

        log.debug("Portfolio calculated for user {}: Total Value = {}, Total Return = {}%",
                userId, response.getTotalValue(), response.getTotalReturnPercent());

        return response;
    }

    /**
     * Build a HoldingResponse with calculated values.
     */
    private HoldingResponse buildHoldingResponse(Holding holding, StockQuote quote) {
        BigDecimal lastPrice = quote != null && quote.getRegularMarketPrice() != null
                ? quote.getRegularMarketPrice()
                : BigDecimal.ZERO;

        BigDecimal shares = holding.getShares();
        BigDecimal avgCost = holding.getAverageCost();

        // Calculate current value and cost basis
        BigDecimal currentValue = lastPrice.multiply(shares);
        BigDecimal costBasis = avgCost.multiply(shares);

        // Calculate returns
        BigDecimal returnDollars = currentValue.subtract(costBasis);
        BigDecimal returnPercent = BigDecimal.ZERO;

        if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
            returnPercent = returnDollars
                    .divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return HoldingResponse.builder()
                .id(holding.getId())
                .symbol(holding.getSymbol())
                .companyName(holding.getCompanyName())
                .shares(shares)
                .averageCost(avgCost)
                .lastPrice(lastPrice)
                .previousClose(quote != null ? quote.getRegularMarketPreviousClose() : null)
                .currentValue(currentValue)
                .costBasis(costBasis)
                .totalReturnDollars(returnDollars)
                .totalReturnPercent(returnPercent)
                .build();
    }

    /**
     * Build a PortfolioResponse with aggregated totals.
     */
    private PortfolioResponse buildPortfolioResponse(List<HoldingResponse> holdings) {
        BigDecimal totalValue = holdings.stream()
                .map(HoldingResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = holdings.stream()
                .map(HoldingResponse::getCostBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturnDollars = totalValue.subtract(totalCost);
        BigDecimal totalReturnPercent = BigDecimal.ZERO;

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnPercent = totalReturnDollars
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return PortfolioResponse.builder()
                .holdings(holdings)
                .totalValue(totalValue)
                .totalCost(totalCost)
                .totalReturnDollars(totalReturnDollars)
                .totalReturnPercent(totalReturnPercent)
                .pricesUpdatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Calculate 7-day return for a holding.
     * Requires fetching historical price from 7 days ago.
     */
    private void calculate7DayReturn(HoldingResponse holding, HistoricalData historicalData) {
        if (historicalData == null || historicalData.getPrices().isEmpty()) {
            holding.setSevenDayReturnPercent(BigDecimal.ZERO);
            holding.setSevenDayReturnDollars(BigDecimal.ZERO);
            return;
        }

        List<HistoricalPrice> prices = historicalData.getPrices();
        BigDecimal currentPrice = holding.getLastPrice();
        BigDecimal price7DaysAgo = prices.get(0).getClose();

        BigDecimal change = currentPrice.subtract(price7DaysAgo);
        BigDecimal changePercent = price7DaysAgo.compareTo(BigDecimal.ZERO) > 0
            ? change.divide(price7DaysAgo, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        holding.setSevenDayReturnPercent(changePercent);
        holding.setSevenDayReturnDollars(change.multiply(holding.getShares()));
    }

    /**
     * Calculate portfolio weight for a holding.
     */
    private void calculateWeight(HoldingResponse holding, BigDecimal totalPortfolioValue) {
        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal weight = holding.getCurrentValue()
                .divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            holding.setWeight(weight);
        } else {
            holding.setWeight(BigDecimal.ZERO);
        }
    }

    /**
     * Extract sparkline data from historical prices (downsample to 52 weekly points).
     */
    private List<BigDecimal> extractSparklineData(HistoricalData historicalData) {
        if (historicalData == null || historicalData.getPrices().isEmpty()) {
            return Collections.emptyList();
        }

        List<HistoricalPrice> prices = historicalData.getPrices();
        // Downsample to ~52 points (weekly) for 1Y data
        int step = Math.max(1, prices.size() / 52);
        List<BigDecimal> sparkline = new ArrayList<>();

        for (int i = 0; i < prices.size(); i += step) {
            sparkline.add(prices.get(i).getClose());
        }

        return sparkline;
    }

    /**
     * Get portfolio performance history for charting.
     */
    @Cacheable(value = "performanceHistory", key = "#userId + '-' + #range")
    public List<PortfolioPerformancePoint> getPerformanceHistory(Long userId, String range) {
        List<Holding> holdings = holdingRepository.findByUserIdOrderBySymbolAsc(userId);

        if (holdings.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch historical data for all symbols
        List<String> symbols = holdings.stream()
            .map(Holding::getSymbol)
            .collect(Collectors.toList());

        Map<String, HistoricalData> historicalDataMap = yahooFinanceClient.getHistoricalDataBatch(symbols, range);

        // Aggregate daily portfolio values
        return aggregatePortfolioPerformance(holdings, historicalDataMap);
    }

    /**
     * Aggregate historical prices into portfolio performance points.
     */
    private List<PortfolioPerformancePoint> aggregatePortfolioPerformance(
            List<Holding> holdings,
            Map<String, HistoricalData> historicalDataMap) {

        // Build a map of date -> sum of (shares * close price)
        Map<LocalDate, BigDecimal> dailyValues = new TreeMap<>();

        for (Holding holding : holdings) {
            HistoricalData data = historicalDataMap.get(holding.getSymbol());
            if (data == null || data.getPrices().isEmpty()) continue;

            for (HistoricalPrice price : data.getPrices()) {
                BigDecimal holdingValue = holding.getShares().multiply(price.getClose());
                dailyValues.merge(price.getDate(), holdingValue, BigDecimal::add);
            }
        }

        // Convert to PortfolioPerformancePoint list with daily changes
        List<PortfolioPerformancePoint> performance = new ArrayList<>();
        BigDecimal previousValue = null;

        for (Map.Entry<LocalDate, BigDecimal> entry : dailyValues.entrySet()) {
            BigDecimal currentValue = entry.getValue();
            BigDecimal dailyChange = previousValue != null
                ? currentValue.subtract(previousValue)
                : BigDecimal.ZERO;
            BigDecimal dailyChangePercent = previousValue != null && previousValue.compareTo(BigDecimal.ZERO) > 0
                ? dailyChange.divide(previousValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

            performance.add(PortfolioPerformancePoint.builder()
                .date(entry.getKey())
                .totalValue(currentValue)
                .dailyChange(dailyChange)
                .dailyChangePercent(dailyChangePercent)
                .build());

            previousValue = currentValue;
        }

        return performance;
    }
}
