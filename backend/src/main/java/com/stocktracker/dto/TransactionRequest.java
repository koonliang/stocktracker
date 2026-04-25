package com.stocktracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotNull LocalDate date,
    @NotBlank @Pattern(regexp = "^[A-Z]{1,5}$") String ticker,
    @NotBlank @Pattern(regexp = "^(buy|sell)$") String type,
    @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.0001") BigDecimal price,
    @NotNull @DecimalMin(value = "0.0") BigDecimal fees) {}
