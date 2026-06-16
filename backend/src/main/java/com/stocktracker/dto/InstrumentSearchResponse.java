package com.stocktracker.dto;

import java.util.List;

public record InstrumentSearchResponse(List<Result> results) {
  public record Result(String symbol, String name, String exchange, String currency) {}
}
