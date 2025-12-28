package com.stocktracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    private List<HoldingResponse> holdings;

    // Portfolio summary (computed on backend)
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalReturnDollars;
    private BigDecimal totalReturnPercent;

    private LocalDateTime pricesUpdatedAt;
}
