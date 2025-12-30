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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioService focusing on new dashboard features:
 * - 7-day return calculation
 * - Portfolio weight calculation
 * - Sparkline data extraction
 * - Performance history aggregation
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PortfolioServiceTest.class);

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private YahooFinanceClient yahooFinanceClient;

    @InjectMocks
    private PortfolioService portfolioService;

    private Holding testHolding1;
    private Holding testHolding2;
    private StockQuote testQuote1;
    private StockQuote testQuote2;

    @BeforeEach
    void setUp() {
        // Setup test data
        testHolding1 = new Holding();
        testHolding1.setId(1L);
        testHolding1.setSymbol("AAPL");
        testHolding1.setCompanyName("Apple Inc.");
        testHolding1.setShares(new BigDecimal("50"));
        testHolding1.setAverageCost(new BigDecimal("150.00"));

        testHolding2 = new Holding();
        testHolding2.setId(2L);
        testHolding2.setSymbol("MSFT");
        testHolding2.setCompanyName("Microsoft Corporation");
        testHolding2.setShares(new BigDecimal("30"));
        testHolding2.setAverageCost(new BigDecimal("300.00"));

        testQuote1 = StockQuote.builder()
                .symbol("AAPL")
                .shortName("Apple Inc.")
                .regularMarketPrice(new BigDecimal("180.00"))
                .regularMarketPreviousClose(new BigDecimal("178.00"))
                .regularMarketChange(new BigDecimal("2.00"))
                .regularMarketChangePercent(new BigDecimal("1.12"))
                .build();

        testQuote2 = StockQuote.builder()
                .symbol("MSFT")
                .shortName("Microsoft Corporation")
                .regularMarketPrice(new BigDecimal("350.00"))
                .regularMarketPreviousClose(new BigDecimal("345.00"))
                .regularMarketChange(new BigDecimal("5.00"))
                .regularMarketChangePercent(new BigDecimal("1.45"))
                .build();
    }

    @Test
    void testGetPortfolio_WithNewFields() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1, testHolding2));

        Map<String, StockQuote> quotes = new HashMap<>();
        quotes.put("AAPL", testQuote1);
        quotes.put("MSFT", testQuote2);
        when(yahooFinanceClient.getQuotes(anyList())).thenReturn(quotes);

        // Mock 7-day historical data
        HistoricalData historical7dAAPL = createHistoricalData("AAPL", "7d",
                Arrays.asList(
                        createHistoricalPrice(LocalDate.now().minusDays(7), "170.00"),
                        createHistoricalPrice(LocalDate.now().minusDays(6), "172.00"),
                        createHistoricalPrice(LocalDate.now().minusDays(5), "175.00")
                ));

        HistoricalData historical7dMSFT = createHistoricalData("MSFT", "7d",
                Arrays.asList(
                        createHistoricalPrice(LocalDate.now().minusDays(7), "340.00"),
                        createHistoricalPrice(LocalDate.now().minusDays(6), "342.00")
                ));

        Map<String, HistoricalData> historical7dMap = new HashMap<>();
        historical7dMap.put("AAPL", historical7dAAPL);
        historical7dMap.put("MSFT", historical7dMSFT);

        // Mock 1-year historical data for sparklines
        HistoricalData historical1yAAPL = createHistoricalData("AAPL", "1y",
                createYearlyPrices(100));
        HistoricalData historical1yMSFT = createHistoricalData("MSFT", "1y",
                createYearlyPrices(100));

        Map<String, HistoricalData> historical1yMap = new HashMap<>();
        historical1yMap.put("AAPL", historical1yAAPL);
        historical1yMap.put("MSFT", historical1yMSFT);

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("7d")))
                .thenReturn(historical7dMap);
        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("1y")))
                .thenReturn(historical1yMap);

        // Act
        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getHoldings().size());

        // Verify AAPL holding
        HoldingResponse aaplHolding = response.getHoldings().stream()
                .filter(h -> "AAPL".equals(h.getSymbol()))
                .findFirst()
                .orElseThrow();

        assertNotNull(aaplHolding.getSevenDayReturnPercent(), "7D return percent should be set");
        assertNotNull(aaplHolding.getSevenDayReturnDollars(), "7D return dollars should be set");
        assertNotNull(aaplHolding.getWeight(), "Weight should be set");
        assertNotNull(aaplHolding.getSparklineData(), "Sparkline data should be set");
        assertFalse(aaplHolding.getSparklineData().isEmpty(), "Sparkline should have data");

        // Verify weight calculation (AAPL: 50 * 180 = 9000, MSFT: 30 * 350 = 10500, Total = 19500)
        BigDecimal expectedAAPLWeight = new BigDecimal("46.15"); // 9000/19500 * 100 = 46.15%
        assertEquals(expectedAAPLWeight.setScale(2, BigDecimal.ROUND_HALF_UP),
                     aaplHolding.getWeight().setScale(2, BigDecimal.ROUND_HALF_UP));

        log.debug("AAPL 7D Return: {}%", aaplHolding.getSevenDayReturnPercent());
        log.debug("AAPL Weight: {}%", aaplHolding.getWeight());
        log.debug("AAPL Sparkline points: {}", aaplHolding.getSparklineData().size());
    }

    @Test
    void testGetPortfolio_Calculate7DayReturn() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1));

        Map<String, StockQuote> quotes = new HashMap<>();
        quotes.put("AAPL", testQuote1);
        when(yahooFinanceClient.getQuotes(anyList())).thenReturn(quotes);

        // 7-day historical: started at $160, now at $180
        HistoricalData historical7d = createHistoricalData("AAPL", "7d",
                Arrays.asList(
                        createHistoricalPrice(LocalDate.now().minusDays(7), "160.00")
                ));

        Map<String, HistoricalData> historical7dMap = new HashMap<>();
        historical7dMap.put("AAPL", historical7d);

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("7d")))
                .thenReturn(historical7dMap);
        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("1y")))
                .thenReturn(new HashMap<>());

        // Act
        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Assert
        HoldingResponse holding = response.getHoldings().get(0);

        // 7-day return: (180 - 160) / 160 * 100 = 12.5%
        BigDecimal expectedReturnPercent = new BigDecimal("12.5000");
        assertEquals(expectedReturnPercent, holding.getSevenDayReturnPercent().setScale(4, BigDecimal.ROUND_HALF_UP));

        // 7-day return dollars: (180 - 160) * 50 shares = $1000
        BigDecimal expectedReturnDollars = new BigDecimal("1000.00");
        assertEquals(expectedReturnDollars, holding.getSevenDayReturnDollars().setScale(2, BigDecimal.ROUND_HALF_UP));

        log.debug("✓ 7-day return calculation verified");
    }

    @Test
    void testGetPortfolio_Calculate7DayReturn_NoHistoricalData() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1));

        Map<String, StockQuote> quotes = new HashMap<>();
        quotes.put("AAPL", testQuote1);
        when(yahooFinanceClient.getQuotes(anyList())).thenReturn(quotes);

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("7d")))
                .thenReturn(new HashMap<>());
        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("1y")))
                .thenReturn(new HashMap<>());

        // Act
        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Assert
        HoldingResponse holding = response.getHoldings().get(0);

        // Should default to zero when no historical data
        assertEquals(BigDecimal.ZERO, holding.getSevenDayReturnPercent());
        assertEquals(BigDecimal.ZERO, holding.getSevenDayReturnDollars());

        log.debug("✓ Missing historical data handled correctly");
    }

    @Test
    void testGetPortfolio_CalculateWeight() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1, testHolding2));

        Map<String, StockQuote> quotes = new HashMap<>();
        quotes.put("AAPL", testQuote1);
        quotes.put("MSFT", testQuote2);
        when(yahooFinanceClient.getQuotes(anyList())).thenReturn(quotes);

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), anyString()))
                .thenReturn(new HashMap<>());

        // Act
        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Assert
        // AAPL: 50 shares * $180 = $9,000
        // MSFT: 30 shares * $350 = $10,500
        // Total: $19,500
        // AAPL weight: 9000/19500 * 100 = 46.15%
        // MSFT weight: 10500/19500 * 100 = 53.85%

        HoldingResponse aaplHolding = response.getHoldings().stream()
                .filter(h -> "AAPL".equals(h.getSymbol()))
                .findFirst()
                .orElseThrow();

        HoldingResponse msftHolding = response.getHoldings().stream()
                .filter(h -> "MSFT".equals(h.getSymbol()))
                .findFirst()
                .orElseThrow();

        BigDecimal expectedAAPLWeight = new BigDecimal("46.15");
        BigDecimal expectedMSFTWeight = new BigDecimal("53.85");

        assertEquals(expectedAAPLWeight, aaplHolding.getWeight().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals(expectedMSFTWeight, msftHolding.getWeight().setScale(2, BigDecimal.ROUND_HALF_UP));

        // Weights should add up to 100%
        BigDecimal totalWeight = aaplHolding.getWeight().add(msftHolding.getWeight());
        assertEquals(new BigDecimal("100.00"), totalWeight.setScale(2, BigDecimal.ROUND_HALF_UP));

        log.debug("✓ Weight calculation verified: AAPL={}%, MSFT={}%", aaplHolding.getWeight(), msftHolding.getWeight());
    }

    @Test
    void testGetPortfolio_SparklineDataExtraction() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1));

        Map<String, StockQuote> quotes = new HashMap<>();
        quotes.put("AAPL", testQuote1);
        when(yahooFinanceClient.getQuotes(anyList())).thenReturn(quotes);

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("7d")))
                .thenReturn(new HashMap<>());

        // Create 252 data points (1 year of trading days)
        List<HistoricalPrice> yearlyPrices = createYearlyPrices(252);
        HistoricalData historical1y = createHistoricalData("AAPL", "1y", yearlyPrices);

        Map<String, HistoricalData> historical1yMap = new HashMap<>();
        historical1yMap.put("AAPL", historical1y);

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("1y")))
                .thenReturn(historical1yMap);

        // Act
        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Assert
        HoldingResponse holding = response.getHoldings().get(0);
        List<BigDecimal> sparklineData = holding.getSparklineData();

        assertNotNull(sparklineData);
        assertFalse(sparklineData.isEmpty());

        // Should be downsampled to approximately 52 points (weekly)
        // With 252 data points: step = 252/52 = 4, result = 252/4 = 63 points
        assertTrue(sparklineData.size() >= 50 && sparklineData.size() <= 70,
                "Sparkline should have ~52-63 points (downsampled), got: " + sparklineData.size());

        // Verify all prices are positive
        for (BigDecimal price : sparklineData) {
            assertTrue(price.compareTo(BigDecimal.ZERO) > 0, "All sparkline prices should be positive");
        }

        log.debug("✓ Sparkline data extracted: {} points from 252 data points", sparklineData.size());
    }

    @Test
    void testGetPerformanceHistory_SingleHolding() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1));

        List<HistoricalPrice> prices = Arrays.asList(
                createHistoricalPrice(LocalDate.now().minusDays(3), "170.00"),
                createHistoricalPrice(LocalDate.now().minusDays(2), "175.00"),
                createHistoricalPrice(LocalDate.now().minusDays(1), "180.00")
        );

        HistoricalData historicalData = createHistoricalData("AAPL", "7d", prices);
        Map<String, HistoricalData> historicalMap = new HashMap<>();
        historicalMap.put("AAPL", historicalData);

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("7d")))
                .thenReturn(historicalMap);

        // Act
        List<PortfolioPerformancePoint> performance = portfolioService.getPerformanceHistory(userId, "7d");

        // Assert
        assertNotNull(performance);
        assertEquals(3, performance.size());

        // Verify first point (50 shares * $170 = $8,500)
        PortfolioPerformancePoint point1 = performance.get(0);
        assertEquals(LocalDate.now().minusDays(3), point1.getDate());
        assertEquals(new BigDecimal("8500.00"), point1.getTotalValue().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals(BigDecimal.ZERO, point1.getDailyChange());

        // Verify second point (50 shares * $175 = $8,750)
        PortfolioPerformancePoint point2 = performance.get(1);
        assertEquals(new BigDecimal("8750.00"), point2.getTotalValue().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals(new BigDecimal("250.00"), point2.getDailyChange().setScale(2, BigDecimal.ROUND_HALF_UP));

        log.debug("✓ Performance history calculated correctly");
    }

    @Test
    void testGetPerformanceHistory_MultipleHoldings() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1, testHolding2));

        // AAPL prices
        List<HistoricalPrice> aaplPrices = Arrays.asList(
                createHistoricalPrice(LocalDate.now().minusDays(2), "170.00"),
                createHistoricalPrice(LocalDate.now().minusDays(1), "180.00")
        );

        // MSFT prices
        List<HistoricalPrice> msftPrices = Arrays.asList(
                createHistoricalPrice(LocalDate.now().minusDays(2), "340.00"),
                createHistoricalPrice(LocalDate.now().minusDays(1), "350.00")
        );

        Map<String, HistoricalData> historicalMap = new HashMap<>();
        historicalMap.put("AAPL", createHistoricalData("AAPL", "7d", aaplPrices));
        historicalMap.put("MSFT", createHistoricalData("MSFT", "7d", msftPrices));

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("7d")))
                .thenReturn(historicalMap);

        // Act
        List<PortfolioPerformancePoint> performance = portfolioService.getPerformanceHistory(userId, "7d");

        // Assert
        assertNotNull(performance);
        assertEquals(2, performance.size());

        // Day 1: AAPL (50*170) + MSFT (30*340) = 8500 + 10200 = 18,700
        PortfolioPerformancePoint point1 = performance.get(0);
        assertEquals(new BigDecimal("18700.00"), point1.getTotalValue().setScale(2, BigDecimal.ROUND_HALF_UP));

        // Day 2: AAPL (50*180) + MSFT (30*350) = 9000 + 10500 = 19,500
        PortfolioPerformancePoint point2 = performance.get(1);
        assertEquals(new BigDecimal("19500.00"), point2.getTotalValue().setScale(2, BigDecimal.ROUND_HALF_UP));

        // Daily change: 19500 - 18700 = 800
        assertEquals(new BigDecimal("800.00"), point2.getDailyChange().setScale(2, BigDecimal.ROUND_HALF_UP));

        log.debug("✓ Multi-holding performance aggregation verified");
    }

    @Test
    void testGetPerformanceHistory_EmptyPortfolio() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Collections.emptyList());

        // Act
        List<PortfolioPerformancePoint> performance = portfolioService.getPerformanceHistory(userId, "1mo");

        // Assert
        assertNotNull(performance);
        assertTrue(performance.isEmpty());

        log.debug("✓ Empty portfolio handled correctly");
    }

    @Test
    void testGetPerformanceHistory_DailyChangePercentage() {
        // Arrange
        Long userId = 1L;
        when(holdingRepository.findByUserIdOrderBySymbolAsc(userId))
                .thenReturn(Arrays.asList(testHolding1));

        List<HistoricalPrice> prices = Arrays.asList(
                createHistoricalPrice(LocalDate.now().minusDays(2), "100.00"),
                createHistoricalPrice(LocalDate.now().minusDays(1), "110.00")
        );

        Map<String, HistoricalData> historicalMap = new HashMap<>();
        historicalMap.put("AAPL", createHistoricalData("AAPL", "7d", prices));

        when(yahooFinanceClient.getHistoricalDataBatch(anyList(), eq("7d")))
                .thenReturn(historicalMap);

        // Act
        List<PortfolioPerformancePoint> performance = portfolioService.getPerformanceHistory(userId, "7d");

        // Assert
        assertEquals(2, performance.size());

        // Day 1: 50 shares * $100 = $5,000
        assertEquals(new BigDecimal("5000.00"), performance.get(0).getTotalValue().setScale(2, BigDecimal.ROUND_HALF_UP));

        // Day 2: 50 shares * $110 = $5,500 (10% increase)
        assertEquals(new BigDecimal("5500.00"), performance.get(1).getTotalValue().setScale(2, BigDecimal.ROUND_HALF_UP));
        assertEquals(new BigDecimal("500.00"), performance.get(1).getDailyChange().setScale(2, BigDecimal.ROUND_HALF_UP));

        // Daily change percent: (500 / 5000) * 100 = 10%
        BigDecimal expectedChangePercent = new BigDecimal("10.0000");
        assertEquals(expectedChangePercent, performance.get(1).getDailyChangePercent().setScale(4, BigDecimal.ROUND_HALF_UP));

        log.debug("✓ Daily change percentage calculated correctly: {}%", performance.get(1).getDailyChangePercent());
    }

    // Helper methods

    private HistoricalData createHistoricalData(String symbol, String range, List<HistoricalPrice> prices) {
        return HistoricalData.builder()
                .symbol(symbol)
                .prices(prices)
                .currentPrice(prices.isEmpty() ? BigDecimal.ZERO : prices.get(prices.size() - 1).getClose())
                .previousClose(prices.size() > 1 ? prices.get(prices.size() - 2).getClose() : BigDecimal.ZERO)
                .build();
    }

    private HistoricalPrice createHistoricalPrice(LocalDate date, String closePrice) {
        BigDecimal close = new BigDecimal(closePrice);
        return HistoricalPrice.builder()
                .date(date)
                .close(close)
                .open(close.subtract(new BigDecimal("1.00")))
                .high(close.add(new BigDecimal("2.00")))
                .low(close.subtract(new BigDecimal("2.00")))
                .volume(1000000L)
                .build();
    }

    private List<HistoricalPrice> createYearlyPrices(int count) {
        List<HistoricalPrice> prices = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(count);

        for (int i = 0; i < count; i++) {
            BigDecimal price = new BigDecimal("150.00").add(new BigDecimal(i * 0.5));
            prices.add(createHistoricalPrice(startDate.plusDays(i), price.toString()));
        }

        return prices;
    }
}
