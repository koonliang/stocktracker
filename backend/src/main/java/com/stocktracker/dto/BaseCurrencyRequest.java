package com.stocktracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BaseCurrencyRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String baseCurrency) {}
