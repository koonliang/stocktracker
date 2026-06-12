package com.stocktracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A transaction to create. Field usage varies by {@code type} (see data-model.md); per-type rules
 * are enforced by {@code TransactionValidationService}, so most numeric fields are optional here
 * and validated only when present. {@code ticker} allows global symbols (e.g. {@code D05.SI}).
 */
public record TransactionRequest(
    @NotNull LocalDate date,
    @Pattern(regexp = "^[A-Z0-9.]{1,16}$") String ticker,
    @NotBlank @Pattern(regexp = "^(buy|sell|dividend|split|deposit|withdrawal|fee)$") String type,
    @DecimalMin(value = "0.000001") BigDecimal quantity,
    @DecimalMin(value = "0.0001") BigDecimal price,
    @DecimalMin(value = "0.0") BigDecimal fees,
    @DecimalMin(value = "0.0001") BigDecimal amount,
    @Pattern(regexp = "^[A-Z]{3}$") String currency) {}
