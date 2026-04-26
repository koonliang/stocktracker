package com.stocktracker.dto;

import java.util.List;
import java.util.Map;

public record TransactionImportPreviewResponse(
    List<ValidRow> validRows, List<InvalidRow> invalidRows, List<String> headerErrors) {
  public record ValidRow(int row, TransactionRequest normalized) {}

  public record InvalidRow(int row, String reason, Map<String, String> raw) {}
}
