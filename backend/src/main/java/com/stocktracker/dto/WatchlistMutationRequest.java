package com.stocktracker.dto;

import java.util.List;

public record WatchlistMutationRequest(String name, String ticker, List<String> tickers) {}
