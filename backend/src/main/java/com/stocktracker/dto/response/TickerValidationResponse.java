package com.stocktracker.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickerValidationResponse {

    private boolean valid;
    private String symbol;
    private String companyName;
    private String errorMessage;
}
