package com.stocktracker.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CsvMappingSuggestionRequest {
    @NotEmpty(message = "Headers cannot be empty")
    private List<String> headers;

    public CsvMappingSuggestionRequest() {
    }

    public CsvMappingSuggestionRequest(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
}
