package com.stocktracker.dto.response;

import java.util.List;

public class CsvImportPreviewResponse {
    private List<TransactionPreviewRow> validRows;
    private List<TransactionPreviewRow> errorRows;
    private int totalRows;
    private int validCount;
    private int errorCount;

    public CsvImportPreviewResponse() {
    }

    public List<TransactionPreviewRow> getValidRows() {
        return validRows;
    }

    public void setValidRows(List<TransactionPreviewRow> validRows) {
        this.validRows = validRows;
    }

    public List<TransactionPreviewRow> getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(List<TransactionPreviewRow> errorRows) {
        this.errorRows = errorRows;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidCount() {
        return validCount;
    }

    public void setValidCount(int validCount) {
        this.validCount = validCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
}
