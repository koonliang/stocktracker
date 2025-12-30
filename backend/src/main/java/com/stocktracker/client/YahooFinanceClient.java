package com.stocktracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktracker.client.dto.HistoricalData;
import com.stocktracker.client.dto.HistoricalPrice;
import com.stocktracker.client.dto.StockQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    /**
     * Fetch historical price data for a symbol.
     * @param symbol Stock symbol
     * @param range Time range (7d, 1mo, 3mo, ytd, 1y)
     * @return HistoricalData with daily prices
     */
    public HistoricalData getHistoricalData(String symbol, String range) {
        try {
            String url = chartUrl + "/" + symbol + "?interval=1d&range=" + range;
            log.debug("Fetching historical data for {} with range {}: {}", symbol, range, url);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.error("Received null response for historical data: {}", symbol);
                return null;
            }

            return parseHistoricalResponse(response, symbol);

        } catch (Exception e) {
            log.error("Failed to fetch historical data for symbol {} with range {}: {}", symbol, range, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch historical data for multiple symbols in parallel.
     * @param symbols List of stock symbols
     * @param range Time range
     * @return Map of symbol to HistoricalData
     */
    public Map<String, HistoricalData> getHistoricalDataBatch(List<String> symbols, String range) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("No symbols provided to fetch historical data");
            return Collections.emptyMap();
        }

        log.debug("Fetching historical data for {} symbols with range {}: {}", symbols.size(), range, symbols);

        // Fetch all symbols in parallel
        List<CompletableFuture<Map.Entry<String, HistoricalData>>> futures = symbols.stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> {
                    HistoricalData data = getHistoricalData(symbol, range);
                    return data != null ? Map.entry(symbol, data) : null;
                }, executor))
                .collect(Collectors.toList());

        // Collect results
        Map<String, HistoricalData> historicalDataMap = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.debug("Successfully fetched historical data for {} out of {} symbols", historicalDataMap.size(), symbols.size());
        return historicalDataMap;
    }

    /**
     * Parse the v8 chart API response for historical data.
     * Response structure includes timestamp array and indicators.quote array with OHLCV data.
     */
    private HistoricalData parseHistoricalResponse(String response, String symbol) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode chart = root.path("chart");
            JsonNode results = chart.path("result");

            if (!results.isArray() || results.isEmpty()) {
                log.warn("No results found for historical data: {}", symbol);
                return null;
            }

            JsonNode result = results.get(0);
            JsonNode meta = result.path("meta");

            // Extract metadata
            BigDecimal currentPrice = getBigDecimalValue(meta, "regularMarketPrice");
            BigDecimal previousClose = getBigDecimalValue(meta, "chartPreviousClose");

            // Extract timestamps
            JsonNode timestampsNode = result.path("timestamp");
            if (!timestampsNode.isArray()) {
                log.warn("No timestamps found for symbol: {}", symbol);
                return HistoricalData.builder()
                        .symbol(symbol)
                        .prices(Collections.emptyList())
                        .currentPrice(currentPrice)
                        .previousClose(previousClose)
                        .build();
            }

            // Extract OHLCV data
            JsonNode indicators = result.path("indicators");
            JsonNode quote = indicators.path("quote");
            if (!quote.isArray() || quote.isEmpty()) {
                log.warn("No quote data found for symbol: {}", symbol);
                return HistoricalData.builder()
                        .symbol(symbol)
                        .prices(Collections.emptyList())
                        .currentPrice(currentPrice)
                        .previousClose(previousClose)
                        .build();
            }

            JsonNode quoteData = quote.get(0);
            JsonNode closeArray = quoteData.path("close");
            JsonNode openArray = quoteData.path("open");
            JsonNode highArray = quoteData.path("high");
            JsonNode lowArray = quoteData.path("low");
            JsonNode volumeArray = quoteData.path("volume");

            // Build price list
            List<HistoricalPrice> prices = new ArrayList<>();
            for (int i = 0; i < timestampsNode.size(); i++) {
                try {
                    long timestamp = timestampsNode.get(i).asLong();
                    LocalDate date = Instant.ofEpochSecond(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    BigDecimal close = getArrayBigDecimalValue(closeArray, i);
                    BigDecimal open = getArrayBigDecimalValue(openArray, i);
                    BigDecimal high = getArrayBigDecimalValue(highArray, i);
                    BigDecimal low = getArrayBigDecimalValue(lowArray, i);
                    Long volume = getArrayLongValue(volumeArray, i);

                    // Skip if close price is null (market was closed or data is missing)
                    if (close != null) {
                        prices.add(HistoricalPrice.builder()
                                .date(date)
                                .close(close)
                                .open(open)
                                .high(high)
                                .low(low)
                                .volume(volume)
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse historical price at index {} for symbol {}: {}", i, symbol, e.getMessage());
                }
            }

            log.debug("Parsed {} historical prices for {}", prices.size(), symbol);

            return HistoricalData.builder()
                    .symbol(symbol)
                    .prices(prices)
                    .currentPrice(currentPrice)
                    .previousClose(previousClose)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse historical response for symbol {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private BigDecimal getArrayBigDecimalValue(JsonNode arrayNode, int index) {
        if (!arrayNode.isArray() || index >= arrayNode.size()) {
            return null;
        }
        JsonNode valueNode = arrayNode.get(index);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(valueNode.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getArrayLongValue(JsonNode arrayNode, int index) {
        if (!arrayNode.isArray() || index >= arrayNode.size()) {
            return null;
        }
        JsonNode valueNode = arrayNode.get(index);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return valueNode.asLong();
    }
}
