package com.stocktracker.dto.response;

public class CsvImportError {
    private Integer rowNumber;
    private String field;
    private String message;
    private String rejectedValue;

    public CsvImportError() {
    }

    public CsvImportError(Integer rowNumber, String field, String message, String rejectedValue) {
        this.rowNumber = rowNumber;
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRejectedValue() {
        return rejectedValue;
    }

    public void setRejectedValue(String rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
}
