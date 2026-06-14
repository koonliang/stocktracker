package com.stocktracker.dto;

import java.time.Instant;

public record AddInstrumentResponse(
    String symbol, String name, String exchange, String currency, QuoteSummary quote) {
  public record QuoteSummary(Double price, Instant asOf, boolean stale) {}
}
