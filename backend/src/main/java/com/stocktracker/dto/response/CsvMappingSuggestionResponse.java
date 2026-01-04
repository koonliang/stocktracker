package com.stocktracker.dto.response;

import java.util.List;
import java.util.Map;

public class CsvMappingSuggestionResponse {
    private Map<String, String> suggestedMappings;
    private Map<String, Double> confidenceScores;
    private List<String> unmappedColumns;

    public CsvMappingSuggestionResponse() {
    }

    public CsvMappingSuggestionResponse(Map<String, String> suggestedMappings,
                                        Map<String, Double> confidenceScores,
                                        List<String> unmappedColumns) {
        this.suggestedMappings = suggestedMappings;
        this.confidenceScores = confidenceScores;
        this.unmappedColumns = unmappedColumns;
    }

    public Map<String, String> getSuggestedMappings() {
        return suggestedMappings;
    }

    public void setSuggestedMappings(Map<String, String> suggestedMappings) {
        this.suggestedMappings = suggestedMappings;
    }

    public Map<String, Double> getConfidenceScores() {
        return confidenceScores;
    }

    public void setConfidenceScores(Map<String, Double> confidenceScores) {
        this.confidenceScores = confidenceScores;
    }

    public List<String> getUnmappedColumns() {
        return unmappedColumns;
    }

    public void setUnmappedColumns(List<String> unmappedColumns) {
        this.unmappedColumns = unmappedColumns;
    }
}
