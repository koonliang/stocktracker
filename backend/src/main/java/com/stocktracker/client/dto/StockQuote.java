package com.stocktracker.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuote {
    private String symbol;
    private String shortName;
    private BigDecimal regularMarketPrice;
    private BigDecimal regularMarketPreviousClose;
    private BigDecimal regularMarketChange;
    private BigDecimal regularMarketChangePercent;
}
