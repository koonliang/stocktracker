package com.stocktracker.service;

import com.stocktracker.dto.TransactionImportPreviewResponse;
import com.stocktracker.dto.TransactionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

@ApplicationScoped
public class TransactionImportService {
  private static final List<String> REQUIRED_HEADERS =
      List.of("date", "ticker", "type", "quantity", "price");
  private static final List<String> CANONICAL_RAW_FIELDS =
      List.of("date", "ticker", "type", "quantity", "price", "fees", "amount", "currency");

  @Inject PortfolioService portfolioService;
  @Inject TransactionValidationService transactionValidationService;

  public TransactionImportPreviewResponse preview(String csvText) {
    var validRows = new ArrayList<TransactionImportPreviewResponse.ValidRow>();
    var invalidRows = new ArrayList<TransactionImportPreviewResponse.InvalidRow>();
    var headerErrors = new ArrayList<String>();

    try {
      var parser =
          CSVParser.parse(
              new StringReader(stripBom(csvText)),
              CSVFormat.DEFAULT
                  .builder()
                  .setHeader()
                  .setSkipHeaderRecord(true)
                  .setIgnoreEmptyLines(true)
                  .setTrim(true)
                  .build());

      var headerMap = parser.getHeaderMap();
      for (var required : REQUIRED_HEADERS) {
        if (!headerMap.containsKey(required)) {
          headerErrors.add("missing required column: " + required);
        }
      }
      if (!headerMap.containsKey("fees")) {
        // optional
      }
      if (!headerErrors.isEmpty()) {
        return new TransactionImportPreviewResponse(validRows, invalidRows, headerErrors, "unknown");
      }

      var records = parser.getRecords();
      var detectedVersion = detectVersion(headerMap.keySet(), records);
      var balances = portfolioService.currentShareBalances();
      for (CSVRecord record : records) {
        var raw = raw(record);
        try {
          var request =
              transactionValidationService.normalize(
                  new TransactionRequest(
                      LocalDate.parse(raw.get("date")),
                      blankToNull(raw.get("ticker")) == null
                          ? null
                          : raw.get("ticker").trim().toUpperCase(Locale.ROOT),
                      raw.get("type").trim().toLowerCase(Locale.ROOT),
                      decimalOrNull(raw.get("quantity")),
                      decimalOrNull(raw.get("price")),
                      raw.get("fees").isBlank() ? BigDecimal.ZERO : new BigDecimal(raw.get("fees")),
                      decimalOrNull(raw.get("amount")),
                      blankToNull(raw.get("currency")) == null
                          ? null
                          : raw.get("currency").trim().toUpperCase(Locale.ROOT)));
          var issue = transactionValidationService.validate(request, balances);
          if (issue != null) {
            invalidRows.add(
                new TransactionImportPreviewResponse.InvalidRow(
                    (int) record.getRecordNumber() + 1, issue, raw));
            continue;
          }
          transactionValidationService.applyToBalances(request, balances);
          validRows.add(
              new TransactionImportPreviewResponse.ValidRow(
                  (int) record.getRecordNumber() + 1, request));
        } catch (Exception exception) {
          invalidRows.add(
              new TransactionImportPreviewResponse.InvalidRow(
                  (int) record.getRecordNumber() + 1,
                  exception.getMessage() == null ? "invalid row" : exception.getMessage(),
                  raw));
        }
      }
      return new TransactionImportPreviewResponse(
          validRows, invalidRows, headerErrors, detectedVersion);
    } catch (Exception exception) {
      headerErrors.add("unable to parse CSV");
    }

    return new TransactionImportPreviewResponse(validRows, invalidRows, headerErrors, "unknown");
  }

  private String detectVersion(java.util.Set<String> headers, List<CSVRecord> records) {
    if (headers.contains("amount") || headers.contains("currency")) {
      return "v2";
    }
    return records.stream()
            .map(record -> record.isMapped("type") ? record.get("type") : "")
            .anyMatch(type -> !type.equalsIgnoreCase("buy") && !type.equalsIgnoreCase("sell"))
        ? "v2"
        : "v1";
  }

  private Map<String, String> raw(CSVRecord record) {
    var raw = new java.util.LinkedHashMap<String, String>();
    for (var field : CANONICAL_RAW_FIELDS) {
      raw.put(field, record.isMapped(field) ? record.get(field) : "");
    }
    return raw;
  }

  private BigDecimal decimalOrNull(String value) {
    return value == null || value.isBlank() ? null : new BigDecimal(value);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String stripBom(String text) {
    if (text != null && !text.isEmpty() && text.charAt(0) == '\ufeff') {
      return text.substring(1);
    }
    return text;
  }
}
