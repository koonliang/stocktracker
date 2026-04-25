package com.stocktracker.service;

import com.stocktracker.dto.TransactionImportPreviewResponse;
import com.stocktracker.dto.TransactionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
              CSVFormat.DEFAULT.builder()
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
        return new TransactionImportPreviewResponse(validRows, invalidRows, headerErrors);
      }

      var balances = portfolioService.currentShareBalances();
      for (CSVRecord record : parser) {
        var raw =
            Map.of(
                "date", record.isMapped("date") ? record.get("date") : "",
                "ticker", record.isMapped("ticker") ? record.get("ticker") : "",
                "type", record.isMapped("type") ? record.get("type") : "",
                "quantity", record.isMapped("quantity") ? record.get("quantity") : "",
                "price", record.isMapped("price") ? record.get("price") : "",
                "fees", record.isMapped("fees") ? record.get("fees") : "");
        try {
          var request =
              transactionValidationService.normalize(
                  new TransactionRequest(
                      LocalDate.parse(raw.get("date")),
                      raw.get("ticker").trim().toUpperCase(Locale.ROOT),
                      raw.get("type").trim().toLowerCase(Locale.ROOT),
                      new BigDecimal(raw.get("quantity")),
                      new BigDecimal(raw.get("price")),
                      raw.get("fees").isBlank() ? BigDecimal.ZERO : new BigDecimal(raw.get("fees"))));
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
    } catch (Exception exception) {
      headerErrors.add("unable to parse CSV");
    }

    return new TransactionImportPreviewResponse(validRows, invalidRows, headerErrors);
  }

  private String stripBom(String text) {
    if (text != null && !text.isEmpty() && text.charAt(0) == '\ufeff') {
      return text.substring(1);
    }
    return text;
  }
}
