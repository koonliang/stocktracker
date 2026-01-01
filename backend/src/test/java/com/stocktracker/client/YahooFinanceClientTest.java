package com.stocktracker.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.client.dto.HistoricalData;
import com.stocktracker.client.dto.HistoricalPrice;
import com.stocktracker.client.dto.StockQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for YahooFinanceClient using v8 chart endpoint.
 */
class YahooFinanceClientTest {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClientTest.class);

    private YahooFinanceClient yahooFinanceClient;
    private static final String YAHOO_CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestTemplateBuilder builder = new RestTemplateBuilder();
        yahooFinanceClient = new YahooFinanceClient(YAHOO_CHART_URL, builder, objectMapper);
    }

    @Test
    void testGetQuotes_SingleSymbol() {
        List<String> symbols = Arrays.asList("AAPL");
        Map<String, StockQuote> quotes = yahooFinanceClient.getQuotes(symbols);

        log.debug("=== Test: Single Symbol (AAPL) ===");
        printQuotes(quotes);

        assertNotNull(quotes);
        if (!quotes.isEmpty()) {
            assertTrue(quotes.containsKey("AAPL"));
            StockQuote quote = quotes.get("AAPL");
            assertNotNull(quote.getRegularMarketPrice());
            log.debug("✓ SUCCESS: Fetched AAPL @ ${}", quote.getRegularMarketPrice());
        }
    }

    @Test
    void testGetQuotes_MultipleSymbols() {
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA", "AMZN");
        Map<String, StockQuote> quotes = yahooFinanceClient.getQuotes(symbols);

        log.debug("=== Test: Multiple Symbols ===");
        printQuotes(quotes);

        assertNotNull(quotes);
        log.debug("✓ Fetched {} out of {} quotes", quotes.size(), symbols.size());
    }

    @Test
    void testRawApiCall_V8Chart() {
        log.debug("=== Direct V8 Chart API Test ===");

        try {
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .defaultHeader("User-Agent", "Mozilla/5.0")
                    .build();
            String url = YAHOO_CHART_URL + "/AAPL?interval=1d&range=1d";

            log.debug("URL: {}", url);
            String response = restTemplate.getForObject(url, String.class);

            assertNotNull(response);
            assertTrue(response.contains("regularMarketPrice"));
            log.debug("✓ V8 Chart API working!");
            log.debug("Response preview: {}...", response.substring(0, Math.min(200, response.length())));

        } catch (Exception e) {
            log.debug("✗ API call failed: {}", e.getMessage());
            fail("V8 Chart API should work");
        }
    }

    @Test
    void testGetHistoricalData_7Days() {
        log.debug("=== Test: Historical Data 7 Days (AAPL) ===");

        HistoricalData historicalData = yahooFinanceClient.getHistoricalData("AAPL", "7d");

        assertNotNull(historicalData, "Historical data should not be null");
        assertEquals("AAPL", historicalData.getSymbol());
        assertNotNull(historicalData.getCurrentPrice(), "Current price should be present");
        assertNotNull(historicalData.getPrices(), "Prices list should not be null");
        assertFalse(historicalData.getPrices().isEmpty(), "Should have historical prices");

        log.debug("Symbol: {}", historicalData.getSymbol());
        log.debug("Current Price: ${}", historicalData.getCurrentPrice());
        log.debug("Number of data points: {}", historicalData.getPrices().size());

        // Verify we have roughly 5-7 days of data (weekdays only)
        assertTrue(historicalData.getPrices().size() >= 3, "Should have at least 3 trading days");
        assertTrue(historicalData.getPrices().size() <= 10, "Should have at most 10 days of data");

        // Verify each price has required fields
        HistoricalPrice firstPrice = historicalData.getPrices().get(0);
        assertNotNull(firstPrice.getDate(), "Date should not be null");
        assertNotNull(firstPrice.getClose(), "Close price should not be null");
        assertTrue(firstPrice.getClose().compareTo(BigDecimal.ZERO) > 0, "Close price should be positive");

        log.debug("First data point: {} @ ${}", firstPrice.getDate(), firstPrice.getClose());
        log.debug("✓ SUCCESS: 7-day historical data fetched");
    }

    @Test
    void testGetHistoricalData_1Year() {
        log.debug("=== Test: Historical Data 1 Year (MSFT) ===");

        HistoricalData historicalData = yahooFinanceClient.getHistoricalData("MSFT", "1y");

        assertNotNull(historicalData, "Historical data should not be null");
        assertEquals("MSFT", historicalData.getSymbol());
        assertNotNull(historicalData.getPrices(), "Prices list should not be null");
        assertFalse(historicalData.getPrices().isEmpty(), "Should have historical prices");

        log.debug("Symbol: {}", historicalData.getSymbol());
        log.debug("Number of data points: {}", historicalData.getPrices().size());

        // Verify we have roughly 252 trading days (1 year)
        assertTrue(historicalData.getPrices().size() >= 200, "Should have at least 200 trading days");
        assertTrue(historicalData.getPrices().size() <= 300, "Should have at most 300 days of data");

        // Verify data is sorted chronologically (oldest first)
        List<HistoricalPrice> prices = historicalData.getPrices();
        if (prices.size() >= 2) {
            assertTrue(prices.get(0).getDate().isBefore(prices.get(prices.size() - 1).getDate()),
                    "Data should be sorted with oldest date first");
        }

        log.debug("Date range: {} to {}", prices.get(0).getDate(),
                prices.get(prices.size() - 1).getDate());
        log.debug("✓ SUCCESS: 1-year historical data fetched");
    }

    @Test
    void testGetHistoricalDataBatch_MultipleSymbols() {
        log.debug("=== Test: Historical Data Batch (AAPL, MSFT, GOOGL) ===");

        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        Map<String, HistoricalData> historicalDataMap =
                yahooFinanceClient.getHistoricalDataBatch(symbols, "1mo");

        assertNotNull(historicalDataMap, "Historical data map should not be null");
        assertFalse(historicalDataMap.isEmpty(), "Should have data for at least some symbols");

        log.debug("Fetched historical data for {} out of {} symbols", historicalDataMap.size(),
                symbols.size());

        // Verify each symbol's data
        historicalDataMap.forEach((symbol, data) -> {
            assertNotNull(data, "Data for " + symbol + " should not be null");
            assertEquals(symbol, data.getSymbol());
            assertFalse(data.getPrices().isEmpty(), symbol + " should have price data");

            log.debug("{}: {} data points, Current: ${}", symbol, data.getPrices().size(),
                    data.getCurrentPrice());
        });

        log.debug("✓ SUCCESS: Batch historical data fetched");
    }

    @Test
    void testGetHistoricalData_InvalidSymbol() {
        log.debug("=== Test: Invalid Symbol ===");

        HistoricalData historicalData = yahooFinanceClient.getHistoricalData("INVALID_SYMBOL_XYZ", "1mo");

        // Should return null or empty prices for invalid symbols
        if (historicalData != null) {
            assertTrue(historicalData.getPrices().isEmpty(),
                    "Invalid symbol should have no price data");
        }

        log.debug("✓ SUCCESS: Invalid symbol handled gracefully");
    }

    @Test
    void testGetHistoricalData_EmptySymbolsList() {
        log.debug("=== Test: Empty Symbols List ===");

        List<String> emptyList = Arrays.asList();
        Map<String, HistoricalData> result = yahooFinanceClient.getHistoricalDataBatch(emptyList, "1mo");

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for empty input");

        log.debug("✓ SUCCESS: Empty list handled correctly");
    }

    @Test
    void testGetHistoricalData_PriceFields() {
        log.debug("=== Test: Price Fields Validation ===");

        HistoricalData historicalData = yahooFinanceClient.getHistoricalData("AAPL", "7d");

        assertNotNull(historicalData, "Historical data should not be null");
        assertFalse(historicalData.getPrices().isEmpty(), "Should have prices");

        // Check a price point has all OHLCV fields
        HistoricalPrice price = historicalData.getPrices().get(0);

        assertNotNull(price.getDate(), "Date should be present");
        assertNotNull(price.getClose(), "Close should be present");
        assertNotNull(price.getOpen(), "Open should be present");
        assertNotNull(price.getHigh(), "High should be present");
        assertNotNull(price.getLow(), "Low should be present");

        // Verify price relationships
        assertTrue(price.getHigh().compareTo(price.getLow()) >= 0,
                "High should be >= Low");
        assertTrue(price.getHigh().compareTo(price.getClose()) >= 0,
                "High should be >= Close");
        assertTrue(price.getLow().compareTo(price.getClose()) <= 0,
                "Low should be <= Close");

        log.debug("Sample OHLCV: O:${} H:${} L:${} C:${} V:{}",
                price.getOpen(), price.getHigh(), price.getLow(), price.getClose(),
                price.getVolume() != null ? price.getVolume() : 0L);
        log.debug("✓ SUCCESS: All price fields validated");
    }

    private void printQuotes(Map<String, StockQuote> quotes) {
        if (quotes.isEmpty()) {
            log.debug("No quotes returned!");
            return;
        }
        quotes.forEach((symbol, quote) -> {
            log.debug("{} ({}): ${} | Prev: ${} | Change: {}%",
                    symbol,
                    quote.getShortName(),
                    quote.getRegularMarketPrice(),
                    quote.getRegularMarketPreviousClose(),
                    quote.getRegularMarketChangePercent());
        });
    }
}
