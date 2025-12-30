package com.stocktracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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

    // NEW: 7D Return
    private BigDecimal sevenDayReturnPercent;
    private BigDecimal sevenDayReturnDollars;

    // NEW: Weight
    private BigDecimal weight;

    // NEW: Sparkline data (1Y daily closes, downsampled to ~52 points for weekly)
    private List<BigDecimal> sparklineData;
}
