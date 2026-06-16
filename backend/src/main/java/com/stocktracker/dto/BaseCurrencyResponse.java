package com.stocktracker.dto;

import java.util.List;

public record BaseCurrencyResponse(String baseCurrency, List<String> supported) {}
