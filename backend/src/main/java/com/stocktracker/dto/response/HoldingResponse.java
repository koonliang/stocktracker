package com.stocktracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {
    private Long id;
    private String symbol;
    private String companyName;
    private BigDecimal shares;
    private BigDecimal averageCost;

    // Live price data (from Yahoo Finance)
    private BigDecimal lastPrice;
    private BigDecimal previousClose;

    // Calculated fields (computed on backend)
    private BigDecimal totalReturnDollars;
    private BigDecimal totalReturnPercent;
    private BigDecimal currentValue;
    private BigDecimal costBasis;
}
