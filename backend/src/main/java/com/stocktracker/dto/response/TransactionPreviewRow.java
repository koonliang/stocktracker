package com.stocktracker.dto.response;

import com.stocktracker.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionPreviewRow {
    private Integer rowNumber;
    private TransactionType type;
    private String symbol;
    private LocalDate transactionDate;
    private BigDecimal shares;
    private BigDecimal pricePerShare;
    private BigDecimal brokerFee;
    private String notes;
    private boolean valid;
    private List<CsvImportError> errors;

    public TransactionPreviewRow() {
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getShares() {
        return shares;
    }

    public void setShares(BigDecimal shares) {
        this.shares = shares;
    }

    public BigDecimal getPricePerShare() {
        return pricePerShare;
    }

    public void setPricePerShare(BigDecimal pricePerShare) {
        this.pricePerShare = pricePerShare;
    }

    public BigDecimal getBrokerFee() {
        return brokerFee;
    }

    public void setBrokerFee(BigDecimal brokerFee) {
        this.brokerFee = brokerFee;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<CsvImportError> getErrors() {
        return errors;
    }

    public void setErrors(List<CsvImportError> errors) {
        this.errors = errors;
    }
}
