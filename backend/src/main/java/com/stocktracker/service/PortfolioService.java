package com.stocktracker.service;

import com.stocktracker.client.YahooFinanceClient;
import com.stocktracker.client.dto.HistoricalData;
import com.stocktracker.client.dto.HistoricalPrice;
import com.stocktracker.client.dto.StockQuote;
import com.stocktracker.dto.response.HoldingResponse;
import com.stocktracker.dto.response.PortfolioPerformancePoint;
import com.stocktracker.dto.response.PortfolioResponse;
import com.stocktracker.entity.Holding;
import com.stocktracker.entity.Transaction;
import com.stocktracker.entity.TransactionType;
import com.stocktracker.repository.HoldingRepository;
import com.stocktracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
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
                    .annualizedYield(BigDecimal.ZERO)
                    .investmentYears(BigDecimal.ZERO)
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
        PortfolioResponse response = buildPortfolioResponse(holdingResponses, userId);

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
    private PortfolioResponse buildPortfolioResponse(List<HoldingResponse> holdings, Long userId) {
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

        PortfolioResponse response = PortfolioResponse.builder()
                .holdings(holdings)
                .totalValue(totalValue)
                .totalCost(totalCost)
                .totalReturnDollars(totalReturnDollars)
                .totalReturnPercent(totalReturnPercent)
                .pricesUpdatedAt(LocalDateTime.now())
                .build();

        // Calculate annualized yield
        calculateAnnualizedYield(response, userId);

        return response;
    }

    /**
     * Calculate annualized yield (CAGR) for the portfolio.
     * Formula: ((1 + totalReturn) ^ (1/years)) - 1
     */
    private void calculateAnnualizedYield(PortfolioResponse response, Long userId) {
        // Get earliest transaction date
        Optional<LocalDate> earliestDate = transactionRepository
                .findEarliestTransactionDateByUserId(userId);

        if (earliestDate.isEmpty() || response.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
            response.setAnnualizedYield(BigDecimal.ZERO);
            response.setInvestmentYears(BigDecimal.ZERO);
            return;
        }

        // Calculate years invested
        long daysBetween = ChronoUnit.DAYS.between(earliestDate.get(), LocalDate.now());
        double years = daysBetween / 365.25;

        if (years < 0.1) {
            // Less than ~36 days, return simple return instead of annualized
            response.setAnnualizedYield(response.getTotalReturnPercent());
            response.setInvestmentYears(BigDecimal.valueOf(years).setScale(2, RoundingMode.HALF_UP));
            return;
        }

        // Calculate total return as decimal (e.g., 0.4158 for 41.58%)
        BigDecimal totalReturnDecimal = response.getTotalReturnPercent()
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);

        // Annualized return: ((1 + r) ^ (1/n)) - 1
        double onePlusReturn = 1 + totalReturnDecimal.doubleValue();
        double annualizedDecimal = Math.pow(onePlusReturn, 1.0 / years) - 1;

        // Convert back to percentage
        BigDecimal annualizedPercent = BigDecimal.valueOf(annualizedDecimal * 100)
                .setScale(2, RoundingMode.HALF_UP);

        response.setAnnualizedYield(annualizedPercent);
        response.setInvestmentYears(BigDecimal.valueOf(years).setScale(1, RoundingMode.HALF_UP));
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
        log.debug("getPerformanceHistory.. userId={}, range={}", userId, range);
        List<Holding> holdings = holdingRepository.findByUserIdOrderBySymbolAsc(userId);

        if (holdings.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch historical data for all symbols
        List<String> symbols = holdings.stream()
            .map(Holding::getSymbol)
            .collect(Collectors.toList());

        // For "all" range, calculate the range from earliest transaction
        String effectiveRange = range;
        if ("all".equalsIgnoreCase(range)) {
            effectiveRange = calculateAllTimeRange(userId);
        }

        Map<String, HistoricalData> historicalDataMap = yahooFinanceClient.getHistoricalDataBatch(symbols, effectiveRange);

        // Aggregate daily portfolio values
        return aggregatePortfolioPerformance(userId, holdings, historicalDataMap);
    }

    /**
     * Calculate the range string for "all" time based on earliest transaction.
     * Returns a range like "5y" or "10y" based on how long the user has been investing.
     */
    private String calculateAllTimeRange(Long userId) {
        Optional<LocalDate> earliestDate = transactionRepository.findEarliestTransactionDateByUserId(userId);

        if (earliestDate.isEmpty()) {
            return "1y"; // Default fallback
        }

        long daysBetween = ChronoUnit.DAYS.between(earliestDate.get(), LocalDate.now());
        long years = daysBetween / 365;

        // Round up to nearest year and add buffer
        if (years < 1) return "1y";
        if (years < 2) return "2y";
        if (years < 5) return "5y";
        if (years < 10) return "10y";
        return "max"; // Yahoo Finance supports "max" for all available data
    }

    /**
     * Aggregate historical prices into portfolio performance points.
     * FIXED: Now uses transaction history to calculate shares owned at each date.
     * Falls back to current holdings if no transaction history exists (backward compatibility).
     */
    private List<PortfolioPerformancePoint> aggregatePortfolioPerformance(
            Long userId,
            List<Holding> holdings,
            Map<String, HistoricalData> historicalDataMap) {

        if (holdings.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch all transactions ordered by date
        List<Transaction> allTransactions = transactionRepository
            .findByUserIdOrderBySymbolAscTransactionDateAsc(userId);

        log.debug("Fetching transactions for userId={}, found {} transactions", userId,
                  allTransactions != null ? allTransactions.size() : 0);

        // If no transaction history, fall back to old behavior (use current holdings)
        if (allTransactions == null || allTransactions.isEmpty()) {
            log.warn("No transaction history found for userId={}. Using fallback calculation with current holdings. " +
                     "This may produce inaccurate historical performance.", userId);
            return aggregatePortfolioPerformanceWithCurrentHoldings(holdings, historicalDataMap);
        }

        log.debug("Using transaction-based calculation for userId={} with {} transactions",
                  userId, allTransactions.size());

        // Build a map of all unique dates from historical data
        // Exclude today's date as historical data may be incomplete
        LocalDate today = LocalDate.now();
        Set<LocalDate> allDates = new TreeSet<>();
        for (HistoricalData data : historicalDataMap.values()) {
            if (data != null && data.getPrices() != null) {
                data.getPrices().stream()
                    .filter(price -> price.getDate().isBefore(today))
                    .forEach(price -> allDates.add(price.getDate()));
            }
        }

        // Pre-build a TreeMap of date -> close price per symbol for carry-forward lookup
        Map<String, TreeMap<LocalDate, BigDecimal>> priceMapBySymbol = new HashMap<>();
        for (Map.Entry<String, HistoricalData> entry : historicalDataMap.entrySet()) {
            TreeMap<LocalDate, BigDecimal> priceMap = new TreeMap<>();
            if (entry.getValue() != null && entry.getValue().getPrices() != null) {
                for (HistoricalPrice price : entry.getValue().getPrices()) {
                    priceMap.put(price.getDate(), price.getClose());
                }
            }
            priceMapBySymbol.put(entry.getKey(), priceMap);
        }

        // Pre-compute shares-per-symbol and cost basis snapshots at each transaction date
        // so we can look up via floorEntry instead of re-scanning all transactions per date
        TreeMap<LocalDate, Map<String, BigDecimal>> sharesSnapshots = new TreeMap<>();
        TreeMap<LocalDate, BigDecimal> costBasisSnapshots = new TreeMap<>();
        precomputeTransactionSnapshots(allTransactions, sharesSnapshots, costBasisSnapshots);

        // For each date, calculate portfolio value and cost basis
        // Skip dates where any held symbol has no price data (no carry-forward available)
        List<PortfolioPerformancePoint> performance = new ArrayList<>();
        BigDecimal previousValue = null;

        for (LocalDate date : allDates) {
            // O(log n) lookup for shares and cost basis at this date
            Map.Entry<LocalDate, Map<String, BigDecimal>> sharesEntry = sharesSnapshots.floorEntry(date);
            Map.Entry<LocalDate, BigDecimal> costEntry = costBasisSnapshots.floorEntry(date);

            if (sharesEntry == null) {
                continue; // No transactions yet
            }

            Map<String, BigDecimal> sharesAtDate = sharesEntry.getValue();
            BigDecimal costBasisAtDate = costEntry != null ? costEntry.getValue() : BigDecimal.ZERO;

            BigDecimal totalValue = BigDecimal.ZERO;
            boolean allPricesAvailable = true;

            for (Map.Entry<String, BigDecimal> entry : sharesAtDate.entrySet()) {
                String symbol = entry.getKey();
                BigDecimal shares = entry.getValue();

                if (shares.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                TreeMap<LocalDate, BigDecimal> priceMap = priceMapBySymbol.get(symbol);
                Map.Entry<LocalDate, BigDecimal> priceEntry = (priceMap != null) ? priceMap.floorEntry(date) : null;

                if (priceEntry == null) {
                    allPricesAvailable = false;
                    break;
                }

                totalValue = totalValue.add(shares.multiply(priceEntry.getValue()));
            }

            if (allPricesAvailable && totalValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyChange = previousValue != null
                    ? totalValue.subtract(previousValue)
                    : BigDecimal.ZERO;
                BigDecimal dailyChangePercent = previousValue != null && previousValue.compareTo(BigDecimal.ZERO) > 0
                    ? dailyChange.divide(previousValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

                performance.add(PortfolioPerformancePoint.builder()
                    .date(date)
                    .totalValue(totalValue)
                    .costBasis(costBasisAtDate.compareTo(BigDecimal.ZERO) > 0 ? costBasisAtDate : BigDecimal.ZERO)
                    .dailyChange(dailyChange)
                    .dailyChangePercent(dailyChangePercent)
                    .build());

                previousValue = totalValue;
            }
        }

        return performance;
    }

    /**
     * Pre-compute snapshots of shares-per-symbol and cumulative cost basis at each transaction date.
     * Transactions are processed in date order. Each transaction date gets a snapshot
     * that can be looked up via TreeMap.floorEntry() for O(log n) per date.
     */
    private void precomputeTransactionSnapshots(
            List<Transaction> transactions,
            TreeMap<LocalDate, Map<String, BigDecimal>> sharesSnapshots,
            TreeMap<LocalDate, BigDecimal> costBasisSnapshots) {

        // Sort transactions by date
        List<Transaction> sorted = transactions.stream()
            .sorted(Comparator.comparing(Transaction::getTransactionDate))
            .collect(Collectors.toList());

        Map<String, BigDecimal> currentShares = new HashMap<>();
        BigDecimal currentCost = BigDecimal.ZERO;

        for (Transaction tx : sorted) {
            String symbol = tx.getSymbol();
            BigDecimal txCost = tx.getShares().multiply(tx.getPricePerShare());

            if (tx.getType() == TransactionType.BUY) {
                currentShares.merge(symbol, tx.getShares(), BigDecimal::add);
                currentCost = currentCost.add(txCost);
            } else if (tx.getType() == TransactionType.SELL) {
                currentShares.merge(symbol, tx.getShares().negate(), BigDecimal::add);
                currentCost = currentCost.subtract(txCost);
            }

            // Store a snapshot (copy) at this transaction date
            sharesSnapshots.put(tx.getTransactionDate(), new HashMap<>(currentShares));
            costBasisSnapshots.put(tx.getTransactionDate(), currentCost);
        }
    }

    /**
     * Fallback method: Use current holdings to calculate historical performance.
     * This is the old behavior, used when no transaction history exists.
     * WARNING: This approach is less accurate as it uses current shares for all dates.
     */
    private List<PortfolioPerformancePoint> aggregatePortfolioPerformanceWithCurrentHoldings(
            List<Holding> holdings,
            Map<String, HistoricalData> historicalDataMap) {

        // Build a TreeMap of date -> close price per symbol for carry-forward lookup
        // Exclude today's date as historical data may be incomplete
        LocalDate today = LocalDate.now();

        Map<String, TreeMap<LocalDate, BigDecimal>> priceMapBySymbol = new HashMap<>();
        Set<LocalDate> allDates = new TreeSet<>();

        for (Holding holding : holdings) {
            HistoricalData data = historicalDataMap.get(holding.getSymbol());
            if (data == null || data.getPrices().isEmpty()) continue;

            TreeMap<LocalDate, BigDecimal> priceMap = new TreeMap<>();
            for (HistoricalPrice price : data.getPrices()) {
                if (price.getDate().isBefore(today)) {
                    priceMap.put(price.getDate(), price.getClose());
                    allDates.add(price.getDate());
                }
            }
            priceMapBySymbol.put(holding.getSymbol(), priceMap);
        }

        // For each date, sum up holdings using carry-forward pricing
        // Skip dates where any holding has no price data available
        Map<LocalDate, BigDecimal> dailyValues = new TreeMap<>();
        for (LocalDate date : allDates) {
            BigDecimal totalValue = BigDecimal.ZERO;
            boolean allPricesAvailable = true;

            for (Holding holding : holdings) {
                TreeMap<LocalDate, BigDecimal> priceMap = priceMapBySymbol.get(holding.getSymbol());
                Map.Entry<LocalDate, BigDecimal> priceEntry = (priceMap != null) ? priceMap.floorEntry(date) : null;

                if (priceEntry == null) {
                    allPricesAvailable = false;
                    break;
                }

                totalValue = totalValue.add(holding.getShares().multiply(priceEntry.getValue()));
            }
            if (allPricesAvailable && totalValue.compareTo(BigDecimal.ZERO) > 0) {
                dailyValues.put(date, totalValue);
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
