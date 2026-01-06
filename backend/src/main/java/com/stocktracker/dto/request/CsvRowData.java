package com.stocktracker.dto.request;

import java.util.Map;

public class CsvRowData {
    private Map<String, String> values;
    private Integer rowNumber;

    public CsvRowData() {
    }

    public CsvRowData(Map<String, String> values, Integer rowNumber) {
        this.values = values;
        this.rowNumber = rowNumber;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }
}
