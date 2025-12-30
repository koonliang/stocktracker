package com.stocktracker.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.client.dto.HistoricalData;
import com.stocktracker.client.dto.HistoricalPrice;
import com.stocktracker.client.dto.StockQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

        System.out.println("=== Test: Single Symbol (AAPL) ===");
        printQuotes(quotes);

        assertNotNull(quotes);
        if (!quotes.isEmpty()) {
            assertTrue(quotes.containsKey("AAPL"));
            StockQuote quote = quotes.get("AAPL");
            assertNotNull(quote.getRegularMarketPrice());
            System.out.println("✓ SUCCESS: Fetched AAPL @ $" + quote.getRegularMarketPrice());
        }
    }

    @Test
    void testGetQuotes_MultipleSymbols() {
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA", "AMZN");
        Map<String, StockQuote> quotes = yahooFinanceClient.getQuotes(symbols);

        System.out.println("=== Test: Multiple Symbols ===");
        printQuotes(quotes);

        assertNotNull(quotes);
        System.out.println("✓ Fetched " + quotes.size() + " out of " + symbols.size() + " quotes");
    }

    @Test
    void testRawApiCall_V8Chart() {
        System.out.println("=== Direct V8 Chart API Test ===");

        try {
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .defaultHeader("User-Agent", "Mozilla/5.0")
                    .build();
            String url = YAHOO_CHART_URL + "/AAPL?interval=1d&range=1d";

            System.out.println("URL: " + url);
            String response = restTemplate.getForObject(url, String.class);

            assertNotNull(response);
            assertTrue(response.contains("regularMarketPrice"));
            System.out.println("✓ V8 Chart API working!");
            System.out.println("Response preview: " + response.substring(0, Math.min(200, response.length())) + "...");

        } catch (Exception e) {
            System.out.println("✗ API call failed: " + e.getMessage());
            fail("V8 Chart API should work");
        }
    }

    @Test
    void testGetHistoricalData_7Days() {
        System.out.println("=== Test: Historical Data 7 Days (AAPL) ===");

        HistoricalData historicalData = yahooFinanceClient.getHistoricalData("AAPL", "7d");

        assertNotNull(historicalData, "Historical data should not be null");
        assertEquals("AAPL", historicalData.getSymbol());
        assertNotNull(historicalData.getCurrentPrice(), "Current price should be present");
        assertNotNull(historicalData.getPrices(), "Prices list should not be null");
        assertFalse(historicalData.getPrices().isEmpty(), "Should have historical prices");

        System.out.println("Symbol: " + historicalData.getSymbol());
        System.out.println("Current Price: $" + historicalData.getCurrentPrice());
        System.out.println("Number of data points: " + historicalData.getPrices().size());

        // Verify we have roughly 5-7 days of data (weekdays only)
        assertTrue(historicalData.getPrices().size() >= 3, "Should have at least 3 trading days");
        assertTrue(historicalData.getPrices().size() <= 10, "Should have at most 10 days of data");

        // Verify each price has required fields
        HistoricalPrice firstPrice = historicalData.getPrices().get(0);
        assertNotNull(firstPrice.getDate(), "Date should not be null");
        assertNotNull(firstPrice.getClose(), "Close price should not be null");
        assertTrue(firstPrice.getClose().compareTo(BigDecimal.ZERO) > 0, "Close price should be positive");

        System.out.println("First data point: " + firstPrice.getDate() + " @ $" + firstPrice.getClose());
        System.out.println("✓ SUCCESS: 7-day historical data fetched");
    }

    @Test
    void testGetHistoricalData_1Year() {
        System.out.println("=== Test: Historical Data 1 Year (MSFT) ===");

        HistoricalData historicalData = yahooFinanceClient.getHistoricalData("MSFT", "1y");

        assertNotNull(historicalData, "Historical data should not be null");
        assertEquals("MSFT", historicalData.getSymbol());
        assertNotNull(historicalData.getPrices(), "Prices list should not be null");
        assertFalse(historicalData.getPrices().isEmpty(), "Should have historical prices");

        System.out.println("Symbol: " + historicalData.getSymbol());
        System.out.println("Number of data points: " + historicalData.getPrices().size());

        // Verify we have roughly 252 trading days (1 year)
        assertTrue(historicalData.getPrices().size() >= 200, "Should have at least 200 trading days");
        assertTrue(historicalData.getPrices().size() <= 300, "Should have at most 300 days of data");

        // Verify data is sorted chronologically (oldest first)
        List<HistoricalPrice> prices = historicalData.getPrices();
        if (prices.size() >= 2) {
            assertTrue(prices.get(0).getDate().isBefore(prices.get(prices.size() - 1).getDate()),
                    "Data should be sorted with oldest date first");
        }

        System.out.println("Date range: " + prices.get(0).getDate() + " to " +
                prices.get(prices.size() - 1).getDate());
        System.out.println("✓ SUCCESS: 1-year historical data fetched");
    }

    @Test
    void testGetHistoricalDataBatch_MultipleSymbols() {
        System.out.println("=== Test: Historical Data Batch (AAPL, MSFT, GOOGL) ===");

        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        Map<String, HistoricalData> historicalDataMap =
                yahooFinanceClient.getHistoricalDataBatch(symbols, "1mo");

        assertNotNull(historicalDataMap, "Historical data map should not be null");
        assertFalse(historicalDataMap.isEmpty(), "Should have data for at least some symbols");

        System.out.println("Fetched historical data for " + historicalDataMap.size() + " out of " +
                symbols.size() + " symbols");

        // Verify each symbol's data
        historicalDataMap.forEach((symbol, data) -> {
            assertNotNull(data, "Data for " + symbol + " should not be null");
            assertEquals(symbol, data.getSymbol());
            assertFalse(data.getPrices().isEmpty(), symbol + " should have price data");

            System.out.printf("%s: %d data points, Current: $%.2f\n",
                    symbol,
                    data.getPrices().size(),
                    data.getCurrentPrice());
        });

        System.out.println("✓ SUCCESS: Batch historical data fetched");
    }

    @Test
    void testGetHistoricalData_InvalidSymbol() {
        System.out.println("=== Test: Invalid Symbol ===");

        HistoricalData historicalData = yahooFinanceClient.getHistoricalData("INVALID_SYMBOL_XYZ", "1mo");

        // Should return null or empty prices for invalid symbols
        if (historicalData != null) {
            assertTrue(historicalData.getPrices().isEmpty(),
                    "Invalid symbol should have no price data");
        }

        System.out.println("✓ SUCCESS: Invalid symbol handled gracefully");
    }

    @Test
    void testGetHistoricalData_EmptySymbolsList() {
        System.out.println("=== Test: Empty Symbols List ===");

        List<String> emptyList = Arrays.asList();
        Map<String, HistoricalData> result = yahooFinanceClient.getHistoricalDataBatch(emptyList, "1mo");

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for empty input");

        System.out.println("✓ SUCCESS: Empty list handled correctly");
    }

    @Test
    void testGetHistoricalData_PriceFields() {
        System.out.println("=== Test: Price Fields Validation ===");

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

        System.out.printf("Sample OHLCV: O:$%.2f H:$%.2f L:$%.2f C:$%.2f V:%d\n",
                price.getOpen(), price.getHigh(), price.getLow(), price.getClose(),
                price.getVolume() != null ? price.getVolume() : 0L);
        System.out.println("✓ SUCCESS: All price fields validated");
    }

    private void printQuotes(Map<String, StockQuote> quotes) {
        if (quotes.isEmpty()) {
            System.out.println("No quotes returned!");
            return;
        }
        quotes.forEach((symbol, quote) -> {
            System.out.printf("%s (%s): $%.2f | Prev: $%.2f | Change: %.2f%%\n",
                    symbol,
                    quote.getShortName(),
                    quote.getRegularMarketPrice(),
                    quote.getRegularMarketPreviousClose(),
                    quote.getRegularMarketChangePercent());
        });
    }
}
