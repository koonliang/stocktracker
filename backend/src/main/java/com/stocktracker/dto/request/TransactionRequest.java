package com.stocktracker.dto.request;

import com.stocktracker.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotBlank(message = "Symbol is required")
    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    private String symbol;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @NotNull(message = "Number of shares is required")
    @Positive(message = "Shares must be positive")
    private BigDecimal shares;

    @NotNull(message = "Price per share is required")
    @Positive(message = "Price must be positive")
    private BigDecimal pricePerShare;

    @PositiveOrZero(message = "Fees cannot be negative")
    private BigDecimal brokerFee;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
