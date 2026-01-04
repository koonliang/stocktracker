package com.stocktracker.service;

import com.stocktracker.dto.request.CsvImportRequest;
import com.stocktracker.dto.request.CsvRowData;
import com.stocktracker.dto.request.TransactionRequest;
import com.stocktracker.dto.response.*;
import com.stocktracker.entity.TransactionType;
import com.stocktracker.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final TransactionService transactionService;

    private static final int MAX_ROWS = 1000;

    // Field mapping aliases
    private static final Map<String, List<String>> FIELD_ALIASES = Map.ofEntries(
        Map.entry("type", Arrays.asList("action", "type", "transaction type", "trade type", "buy/sell", "side", "order type", "transaction")),
        Map.entry("symbol", Arrays.asList("symbol", "ticker", "stock", "security", "instrument", "stock symbol", "ticker symbol", "code")),
        Map.entry("exchange", Arrays.asList("exchange", "listing exchange", "market", "listingexchange", "stock exchange")),
        Map.entry("transactionDate", Arrays.asList("date", "trade date", "transaction date", "settlement date", "exec date", "execution date", "activity date", "tradedate")),
        Map.entry("shares", Arrays.asList("shares", "quantity", "qty", "units", "amount", "volume", "share quantity")),
        Map.entry("pricePerShare", Arrays.asList("price", "share price", "unit price", "execution price", "trade price", "cost per share", "price per share", "t. price")),
        Map.entry("notes", Arrays.asList("notes", "memo", "description", "comment", "remarks"))
    );

    // Exchange code to Yahoo Finance suffix mapping
    private static final Map<String, String> EXCHANGE_SUFFIX_MAP = Map.ofEntries(
        // London Stock Exchange
        Map.entry("LSE", ".L"),
        Map.entry("LSEETF", ".L"),
        Map.entry("LON", ".L"),
        // Hong Kong Stock Exchange
        Map.entry("SEHK", ".HK"),
        Map.entry("HKG", ".HK"),
        // Toronto Stock Exchange
        Map.entry("TSE", ".TO"),
        Map.entry("TSX", ".TO"),
        // Australian Securities Exchange
        Map.entry("ASX", ".AX"),
        // Deutsche Börse
        Map.entry("XETRA", ".DE"),
        Map.entry("FRA", ".F"),
        // Euronext Paris
        Map.entry("EPA", ".PA"),
        // Swiss Exchange
        Map.entry("SIX", ".SW"),
        // Amsterdam
        Map.entry("AMS", ".AS"),
        // Brussels
        Map.entry("EBR", ".BR"),
        // Milan
        Map.entry("MIL", ".MI"),
        // Madrid
        Map.entry("MCE", ".MC"),
        // Copenhagen
        Map.entry("CSE", ".CO"),
        // Stockholm
        Map.entry("STO", ".ST"),
        // Oslo
        Map.entry("OSE", ".OL"),
        // Singapore
        Map.entry("SGX", ".SI"),
        // Tokyo
        Map.entry("TYO", ".T"),
        // No suffix needed for US exchanges
        Map.entry("NASDAQ", ""),
        Map.entry("NYSE", ""),
        Map.entry("AMEX", ""),
        Map.entry("ARCA", "")
    );

    // Transaction type mapping
    private static final Map<String, TransactionType> TYPE_MAPPINGS = Map.ofEntries(
        Map.entry("buy", TransactionType.BUY),
        Map.entry("b", TransactionType.BUY),
        Map.entry("purchase", TransactionType.BUY),
        Map.entry("bought", TransactionType.BUY),
        Map.entry("you bought", TransactionType.BUY),
        Map.entry("bot", TransactionType.BUY),
        Map.entry("sell", TransactionType.SELL),
        Map.entry("s", TransactionType.SELL),
        Map.entry("sale", TransactionType.SELL),
        Map.entry("sold", TransactionType.SELL),
        Map.entry("you sold", TransactionType.SELL),
        Map.entry("sld", TransactionType.SELL)
    );

    // Common date formats
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("M/d/yy"),
        DateTimeFormatter.ofPattern("MM/dd/yy"),
        DateTimeFormatter.ISO_LOCAL_DATE
    );

    /**
     * Suggest field mappings based on CSV headers.
     */
    public CsvMappingSuggestionResponse suggestMappings(List<String> headers) {
        log.debug("Suggesting mappings for headers: {}", headers);

        Map<String, String> suggestedMappings = new HashMap<>();
        Map<String, Double> confidenceScores = new HashMap<>();
        Map<String, String> bestMatchPerField = new HashMap<>();
        Map<String, Double> bestConfidencePerField = new HashMap<>();
        List<String> unmappedColumns = new ArrayList<>();

        // First pass: find the best CSV column for each standard field
        for (String header : headers) {
            String normalizedHeader = header.toLowerCase().trim();

            // Try to find best match
            FieldMatch bestMatch = findBestFieldMatch(normalizedHeader);

            if (bestMatch != null && bestMatch.confidence >= 0.6) {
                String fieldName = bestMatch.fieldName;
                double confidence = bestMatch.confidence;

                // Only keep this mapping if it's better than what we already have for this field
                if (!bestMatchPerField.containsKey(fieldName) ||
                    confidence > bestConfidencePerField.get(fieldName)) {

                    // Remove old mapping if it exists
                    String oldHeader = bestMatchPerField.get(fieldName);
                    if (oldHeader != null) {
                        suggestedMappings.remove(oldHeader);
                        confidenceScores.remove(oldHeader);
                        unmappedColumns.add(oldHeader);
                    }

                    // Add new mapping
                    bestMatchPerField.put(fieldName, header);
                    bestConfidencePerField.put(fieldName, confidence);
                    suggestedMappings.put(header, fieldName);
                    confidenceScores.put(header, confidence);
                } else {
                    unmappedColumns.add(header);
                }
            } else {
                unmappedColumns.add(header);
            }
        }

        return new CsvMappingSuggestionResponse(suggestedMappings, confidenceScores, unmappedColumns);
    }

    /**
     * Preview import with validation.
     */
    public CsvImportPreviewResponse previewImport(CsvImportRequest request) {
        log.debug("Previewing import of {} rows", request.getRows().size());

        if (request.getRows().size() > MAX_ROWS) {
            throw new BadRequestException("Cannot import more than " + MAX_ROWS + " rows at once");
        }

        validateRequiredMappings(request.getFieldMappings());

        List<TransactionPreviewRow> validRows = new ArrayList<>();
        List<TransactionPreviewRow> errorRows = new ArrayList<>();

        for (CsvRowData rowData : request.getRows()) {
            TransactionPreviewRow previewRow = validateAndMapRow(rowData, request.getFieldMappings());
            if (previewRow.isValid()) {
                validRows.add(previewRow);
            } else {
                errorRows.add(previewRow);
            }
        }

        CsvImportPreviewResponse response = new CsvImportPreviewResponse();
        response.setValidRows(validRows);
        response.setErrorRows(errorRows);
        response.setTotalRows(request.getRows().size());
        response.setValidCount(validRows.size());
        response.setErrorCount(errorRows.size());

        return response;
    }

    /**
     * Execute the import of validated transactions.
     * Note: Not transactional - each transaction import has its own transaction.
     */
    public CsvImportResultResponse executeImport(Long userId, CsvImportRequest request) {
        log.info("Executing import for user {} with {} rows", userId, request.getRows().size());

        if (request.getRows().size() > MAX_ROWS) {
            throw new BadRequestException("Cannot import more than " + MAX_ROWS + " rows at once");
        }

        validateRequiredMappings(request.getFieldMappings());

        List<TransactionResponse> importedTransactions = new ArrayList<>();
        List<CsvImportError> errors = new ArrayList<>();
        int skippedCount = 0;

        for (CsvRowData rowData : request.getRows()) {
            try {
                TransactionPreviewRow previewRow = validateAndMapRow(rowData, request.getFieldMappings());

                if (!previewRow.isValid()) {
                    errors.addAll(previewRow.getErrors());
                    skippedCount++;
                    continue;
                }

                // Create transaction request
                TransactionRequest transactionRequest = TransactionRequest.builder()
                    .type(previewRow.getType())
                    .symbol(previewRow.getSymbol())
                    .transactionDate(previewRow.getTransactionDate())
                    .shares(previewRow.getShares())
                    .pricePerShare(previewRow.getPricePerShare())
                    .notes(previewRow.getNotes())
                    .build();

                TransactionResponse created = transactionService.createTransaction(userId, transactionRequest);
                importedTransactions.add(created);

            } catch (BadRequestException e) {
                log.error("Validation error importing row {}: {}", rowData.getRowNumber(), e.getMessage());
                errors.add(new CsvImportError(
                    rowData.getRowNumber(),
                    "symbol",
                    e.getMessage(),
                    null
                ));
                skippedCount++;
            } catch (Exception e) {
                log.error("Error importing row {}: {}", rowData.getRowNumber(), e.getMessage(), e);
                errors.add(new CsvImportError(
                    rowData.getRowNumber(),
                    null,
                    "Import failed: " + e.getMessage(),
                    null
                ));
                skippedCount++;
            }
        }

        CsvImportResultResponse response = new CsvImportResultResponse();
        response.setImportedCount(importedTransactions.size());
        response.setSkippedCount(skippedCount);
        response.setErrors(errors);
        response.setImportedTransactions(importedTransactions);

        log.info("Import completed: {} imported, {} skipped", importedTransactions.size(), skippedCount);
        return response;
    }

    /**
     * Validate and map a single row.
     */
    private TransactionPreviewRow validateAndMapRow(CsvRowData rowData, Map<String, String> fieldMappings) {
        TransactionPreviewRow previewRow = new TransactionPreviewRow();
        previewRow.setRowNumber(rowData.getRowNumber());
        List<CsvImportError> rowErrors = new ArrayList<>();

        try {
            // Extract mapped values
            Map<String, String> mappedValues = extractMappedValues(rowData.getValues(), fieldMappings);

            // Parse shares first (may be negative for SELL transactions)
            String sharesValue = mappedValues.get("shares");
            BigDecimal shares = parseSharesWithSign(sharesValue, rowData.getRowNumber(), rowErrors);

            // Infer type from shares sign if not explicitly provided (IBKR pattern)
            TransactionType type;
            String typeValue = mappedValues.get("type");
            if (typeValue == null || typeValue.isEmpty()) {
                // No explicit type - infer from shares sign
                if (shares != null && shares.compareTo(BigDecimal.ZERO) < 0) {
                    type = TransactionType.SELL;
                    shares = shares.abs(); // Convert to positive
                } else {
                    type = TransactionType.BUY;
                }
            } else {
                // Explicit type provided
                type = parseType(typeValue, rowData.getRowNumber(), rowErrors);
                // If type is SELL and shares are negative, make them positive
                if (type == TransactionType.SELL && shares != null && shares.compareTo(BigDecimal.ZERO) < 0) {
                    shares = shares.abs();
                }
            }

            String exchange = mappedValues.get("exchange");
            String symbol = parseSymbol(mappedValues.get("symbol"), exchange, rowData.getRowNumber(), rowErrors);
            LocalDate transactionDate = parseDate(mappedValues.get("transactionDate"), rowData.getRowNumber(), rowErrors);
            BigDecimal pricePerShare = parsePrice(mappedValues.get("pricePerShare"), rowData.getRowNumber(), rowErrors);
            String notes = mappedValues.get("notes");

            previewRow.setType(type);
            previewRow.setSymbol(symbol);
            previewRow.setTransactionDate(transactionDate);
            previewRow.setShares(shares);
            previewRow.setPricePerShare(pricePerShare);
            previewRow.setNotes(notes);
            previewRow.setValid(rowErrors.isEmpty());
            previewRow.setErrors(rowErrors);

        } catch (Exception e) {
            log.error("Error mapping row {}: {}", rowData.getRowNumber(), e.getMessage());
            rowErrors.add(new CsvImportError(
                rowData.getRowNumber(),
                null,
                "Mapping error: " + e.getMessage(),
                null
            ));
            previewRow.setValid(false);
            previewRow.setErrors(rowErrors);
        }

        return previewRow;
    }

    /**
     * Extract mapped values from row data.
     */
    private Map<String, String> extractMappedValues(Map<String, String> rowValues, Map<String, String> fieldMappings) {
        Map<String, String> mappedValues = new HashMap<>();

        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String csvColumn = mapping.getKey();
            String standardField = mapping.getValue();
            String value = rowValues.get(csvColumn);

            if (value != null && !value.trim().isEmpty()) {
                mappedValues.put(standardField, value.trim());
            }
        }

        return mappedValues;
    }

    /**
     * Parse transaction type.
     */
    private TransactionType parseType(String value, Integer rowNumber, List<CsvImportError> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(new CsvImportError(rowNumber, "type", "Type is required", value));
            return null;
        }

        String normalized = value.toLowerCase().trim();
        TransactionType type = TYPE_MAPPINGS.get(normalized);

        if (type == null) {
            errors.add(new CsvImportError(
                rowNumber,
                "type",
                "Invalid type. Must be BUY or SELL (or common variations)",
                value
            ));
        }

        return type;
    }

    /**
     * Parse symbol with optional exchange suffix.
     */
    private String parseSymbol(String value, String exchange, Integer rowNumber, List<CsvImportError> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(new CsvImportError(rowNumber, "symbol", "Symbol is required", value));
            return null;
        }

        String symbol = value.toUpperCase().trim();

        // Add exchange suffix if exchange is provided and mapped
        if (exchange != null && !exchange.isEmpty()) {
            String exchangeUpper = exchange.toUpperCase().trim();
            String suffix = EXCHANGE_SUFFIX_MAP.get(exchangeUpper);

            if (suffix != null && !suffix.isEmpty()) {
                // Only add suffix if not already present
                if (!symbol.contains(".")) {
                    symbol = symbol + suffix;
                }
            }
        }

        if (symbol.length() > 15 || !symbol.matches("[A-Z0-9.]+")) {
            errors.add(new CsvImportError(
                rowNumber,
                "symbol",
                "Invalid symbol format",
                value
            ));
            return null;
        }

        return symbol;
    }

    /**
     * Parse transaction date.
     */
    private LocalDate parseDate(String value, Integer rowNumber, List<CsvImportError> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(new CsvImportError(rowNumber, "transactionDate", "Date is required", value));
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(value, formatter);

                if (date.isAfter(LocalDate.now())) {
                    errors.add(new CsvImportError(
                        rowNumber,
                        "transactionDate",
                        "Transaction date cannot be in the future",
                        value
                    ));
                    return null;
                }

                return date;
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        errors.add(new CsvImportError(
            rowNumber,
            "transactionDate",
            "Invalid date format. Expected formats: MM/DD/YYYY, YYYY-MM-DD, etc.",
            value
        ));
        return null;
    }

    /**
     * Parse shares (allows negative values for IBKR pattern where negative = sell).
     */
    private BigDecimal parseSharesWithSign(String value, Integer rowNumber, List<CsvImportError> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(new CsvImportError(rowNumber, "shares", "Shares is required", value));
            return null;
        }

        try {
            // Remove commas and other formatting (but keep negative sign)
            String cleaned = value.replaceAll("[,\\s]", "");
            BigDecimal shares = new BigDecimal(cleaned);

            // Allow zero or negative (will be handled by caller)
            if (shares.compareTo(BigDecimal.ZERO) == 0) {
                errors.add(new CsvImportError(
                    rowNumber,
                    "shares",
                    "Shares cannot be zero",
                    value
                ));
                return null;
            }

            return shares;
        } catch (NumberFormatException e) {
            errors.add(new CsvImportError(
                rowNumber,
                "shares",
                "Invalid number format for shares",
                value
            ));
            return null;
        }
    }

    /**
     * Parse shares (legacy method - requires positive values).
     */
    private BigDecimal parseShares(String value, Integer rowNumber, List<CsvImportError> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(new CsvImportError(rowNumber, "shares", "Shares is required", value));
            return null;
        }

        try {
            // Remove commas and other formatting
            String cleaned = value.replaceAll("[,\\s]", "");
            BigDecimal shares = new BigDecimal(cleaned);

            if (shares.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new CsvImportError(
                    rowNumber,
                    "shares",
                    "Shares must be greater than zero",
                    value
                ));
                return null;
            }

            return shares;
        } catch (NumberFormatException e) {
            errors.add(new CsvImportError(
                rowNumber,
                "shares",
                "Invalid number format for shares",
                value
            ));
            return null;
        }
    }

    /**
     * Parse price per share.
     */
    private BigDecimal parsePrice(String value, Integer rowNumber, List<CsvImportError> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(new CsvImportError(rowNumber, "pricePerShare", "Price per share is required", value));
            return null;
        }

        try {
            // Remove currency symbols, commas, and other formatting
            String cleaned = value.replaceAll("[$,\\s]", "");
            BigDecimal price = new BigDecimal(cleaned);

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new CsvImportError(
                    rowNumber,
                    "pricePerShare",
                    "Price per share must be greater than zero",
                    value
                ));
                return null;
            }

            return price;
        } catch (NumberFormatException e) {
            errors.add(new CsvImportError(
                rowNumber,
                "pricePerShare",
                "Invalid number format for price",
                value
            ));
            return null;
        }
    }

    /**
     * Validate that all required fields are mapped.
     * Note: "type" is optional as it can be inferred from shares sign (IBKR pattern).
     */
    private void validateRequiredMappings(Map<String, String> fieldMappings) {
        List<String> requiredFields = Arrays.asList("symbol", "transactionDate", "shares", "pricePerShare");
        List<String> mappedFields = new ArrayList<>(fieldMappings.values());

        List<String> missingFields = requiredFields.stream()
            .filter(field -> !mappedFields.contains(field))
            .collect(Collectors.toList());

        if (!missingFields.isEmpty()) {
            throw new BadRequestException("Missing required field mappings: " + String.join(", ", missingFields));
        }
    }

    /**
     * Find the best field match using fuzzy matching.
     */
    private FieldMatch findBestFieldMatch(String normalizedHeader) {
        FieldMatch bestMatch = null;
        double bestConfidence = 0.0;

        for (Map.Entry<String, List<String>> entry : FIELD_ALIASES.entrySet()) {
            String fieldName = entry.getKey();
            List<String> aliases = entry.getValue();

            for (String alias : aliases) {
                double confidence = calculateConfidence(normalizedHeader, alias);

                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestMatch = new FieldMatch(fieldName, confidence);
                }
            }
        }

        return bestMatch;
    }

    /**
     * Calculate confidence score for field matching.
     */
    private double calculateConfidence(String header, String alias) {
        // Exact match
        if (header.equals(alias)) {
            return 1.0;
        }

        // Contains match
        if (header.contains(alias) || alias.contains(header)) {
            return 0.9;
        }

        // Levenshtein distance
        int distance = levenshteinDistance(header, alias);
        int maxLength = Math.max(header.length(), alias.length());

        if (maxLength == 0) {
            return 0.0;
        }

        double similarity = 1.0 - ((double) distance / maxLength);
        return similarity > 0.7 ? similarity : 0.0;
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Helper class for field matching results.
     */
    private static class FieldMatch {
        String fieldName;
        double confidence;

        FieldMatch(String fieldName, double confidence) {
            this.fieldName = fieldName;
            this.confidence = confidence;
        }
    }
}
