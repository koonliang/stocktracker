package com.stocktracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.client.dto.StockQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@Slf4j
public class YahooFinanceClient {

    private final String chartUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public YahooFinanceClient(
            @Value("${yahoo.finance.chart-url}") String chartUrl,
            RestTemplateBuilder builder,
            ObjectMapper objectMapper) {
        this.chartUrl = chartUrl;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .defaultHeader("Accept", "application/json")
                .build();
        this.objectMapper = objectMapper;
        this.executor = Executors.newFixedThreadPool(5);
    }

    /**
     * Fetch quotes for multiple symbols using v8 chart endpoint.
     * Makes parallel requests since v8 only supports single symbol per request.
     * @param symbols List of stock symbols
     * @return Map of symbol to quote data
     */
    public Map<String, StockQuote> getQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("No symbols provided to fetch quotes");
            return Collections.emptyMap();
        }

        log.debug("Fetching quotes for {} symbols: {}", symbols.size(), symbols);

        // Fetch all symbols in parallel
        List<CompletableFuture<Map.Entry<String, StockQuote>>> futures = symbols.stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> {
                    StockQuote quote = fetchSingleQuote(symbol);
                    return quote != null ? Map.entry(symbol, quote) : null;
                }, executor))
                .collect(Collectors.toList());

        // Collect results
        Map<String, StockQuote> quotes = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.debug("Successfully fetched {} out of {} quotes", quotes.size(), symbols.size());
        return quotes;
    }

    /**
     * Fetch a single quote using the v8 chart endpoint.
     */
    private StockQuote fetchSingleQuote(String symbol) {
        try {
            String url = chartUrl + "/" + symbol + "?interval=1d&range=1d";
            log.debug("Fetching quote for {}: {}", symbol, url);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.error("Received null response for symbol: {}", symbol);
                return null;
            }

            return parseChartResponse(response, symbol);

        } catch (Exception e) {
            log.error("Failed to fetch quote for symbol {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Parse the v8 chart API response.
     * Response structure:
     * {
     *   "chart": {
     *     "result": [{
     *       "meta": {
     *         "symbol": "AAPL",
     *         "regularMarketPrice": 178.25,
     *         "chartPreviousClose": 176.50,
     *         "shortName": "Apple Inc."
     *       }
     *     }]
     *   }
     * }
     */
    private StockQuote parseChartResponse(String response, String symbol) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode chart = root.path("chart");
            JsonNode results = chart.path("result");

            if (!results.isArray() || results.isEmpty()) {
                log.warn("No results found for symbol: {}", symbol);
                return null;
            }

            JsonNode meta = results.get(0).path("meta");

            BigDecimal regularMarketPrice = getBigDecimalValue(meta, "regularMarketPrice");
            BigDecimal previousClose = getBigDecimalValue(meta, "chartPreviousClose");

            // Calculate change values
            BigDecimal change = BigDecimal.ZERO;
            BigDecimal changePercent = BigDecimal.ZERO;

            if (regularMarketPrice != null && previousClose != null && previousClose.compareTo(BigDecimal.ZERO) > 0) {
                change = regularMarketPrice.subtract(previousClose);
                changePercent = change.divide(previousClose, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            StockQuote quote = StockQuote.builder()
                    .symbol(getTextValue(meta, "symbol"))
                    .shortName(getTextValue(meta, "shortName"))
                    .regularMarketPrice(regularMarketPrice)
                    .regularMarketPreviousClose(previousClose)
                    .regularMarketChange(change)
                    .regularMarketChangePercent(changePercent)
                    .build();

            log.debug("Parsed quote for {}: ${} ({}%)",
                    quote.getSymbol(), quote.getRegularMarketPrice(), quote.getRegularMarketChangePercent());

            return quote;

        } catch (Exception e) {
            log.error("Failed to parse chart response for symbol {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : null;
    }

    private BigDecimal getBigDecimalValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            try {
                return new BigDecimal(field.asText());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse {} as BigDecimal: {}", fieldName, field.asText());
            }
        }
        return null;
    }
}
