package com.stocktracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddInstrumentRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9.]{1,16}$") String symbol) {}
