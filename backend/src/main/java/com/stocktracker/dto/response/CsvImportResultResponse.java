package com.stocktracker.dto.response;

import java.util.List;

public class CsvImportResultResponse {
    private int importedCount;
    private int skippedCount;
    private List<CsvImportError> errors;
    private List<TransactionResponse> importedTransactions;

    public CsvImportResultResponse() {
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<CsvImportError> getErrors() {
        return errors;
    }

    public void setErrors(List<CsvImportError> errors) {
        this.errors = errors;
    }

    public List<TransactionResponse> getImportedTransactions() {
        return importedTransactions;
    }

    public void setImportedTransactions(List<TransactionResponse> importedTransactions) {
        this.importedTransactions = importedTransactions;
    }
}
