package com.stocktracker.dto;

import java.time.LocalDate;

public final class ConversionDtos {
  private ConversionDtos() {}

  public enum FxStatus {
    current,
    stale,
    unavailable
  }

  public record ConversionMetadata(
      String baseCurrency, double amountBase, LocalDate fxDate, FxStatus fxStatus) {}
}
