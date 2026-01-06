package com.stocktracker.dto.response;

import com.stocktracker.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private String symbol;
    private String companyName;
    private LocalDate transactionDate;
    private BigDecimal shares;
    private BigDecimal pricePerShare;
    private BigDecimal brokerFee;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
