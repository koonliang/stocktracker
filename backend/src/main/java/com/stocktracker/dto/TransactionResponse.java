package com.stocktracker.dto;

public record TransactionResponse(
    String id,
    String date,
    String ticker,
    String type,
    double quantity,
    double price,
    double fees,
    Double amount,
    String currency,
    String currencySource,
    String source) {}
