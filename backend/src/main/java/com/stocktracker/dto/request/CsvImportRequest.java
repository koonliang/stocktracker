package com.stocktracker.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class CsvImportRequest {
    @NotEmpty(message = "Rows cannot be empty")
    private List<CsvRowData> rows;

    @NotNull(message = "Field mappings are required")
    private Map<String, String> fieldMappings;

    public CsvImportRequest() {
    }

    public CsvImportRequest(List<CsvRowData> rows, Map<String, String> fieldMappings) {
        this.rows = rows;
        this.fieldMappings = fieldMappings;
    }

    public List<CsvRowData> getRows() {
        return rows;
    }

    public void setRows(List<CsvRowData> rows) {
        this.rows = rows;
    }

    public Map<String, String> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(Map<String, String> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }
}
