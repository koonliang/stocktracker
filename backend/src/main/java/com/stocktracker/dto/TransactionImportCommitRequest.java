package com.stocktracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TransactionImportCommitRequest(@NotNull List<@Valid TransactionRequest> rows) {}
