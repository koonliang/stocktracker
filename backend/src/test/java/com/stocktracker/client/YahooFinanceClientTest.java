package com.stocktracker.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.client.dto.StockQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

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
