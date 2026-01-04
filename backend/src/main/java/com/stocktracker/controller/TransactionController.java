package com.stocktracker.controller;

import com.stocktracker.dto.request.CsvImportRequest;
import com.stocktracker.dto.request.CsvMappingSuggestionRequest;
import com.stocktracker.dto.request.TransactionRequest;
import com.stocktracker.dto.response.*;
import com.stocktracker.entity.User;
import com.stocktracker.repository.UserRepository;
import com.stocktracker.service.CsvImportService;
import com.stocktracker.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;
    private final CsvImportService csvImportService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all transactions for the authenticated user")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<TransactionResponse> transactions = transactionService.getTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/validate-ticker")
    @Operation(summary = "Validate a ticker symbol")
    public ResponseEntity<ApiResponse<TickerValidationResponse>> validateTicker(
            @RequestParam String symbol) {
        TickerValidationResponse response = transactionService.validateTicker(symbol);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransactionRequest request) {
        Long userId = getUserId(userDetails);
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        Long userId = getUserId(userDetails);
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/import/suggest-mapping")
    @Operation(summary = "Suggest field mappings for CSV import based on headers")
    public ResponseEntity<ApiResponse<CsvMappingSuggestionResponse>> suggestMapping(
            @Valid @RequestBody CsvMappingSuggestionRequest request) {
        CsvMappingSuggestionResponse response = csvImportService.suggestMappings(request.getHeaders());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/import/preview")
    @Operation(summary = "Preview CSV import with validation")
    public ResponseEntity<ApiResponse<CsvImportPreviewResponse>> previewImport(
            @Valid @RequestBody CsvImportRequest request) {
        CsvImportPreviewResponse response = csvImportService.previewImport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/import")
    @Operation(summary = "Import transactions from CSV")
    public ResponseEntity<ApiResponse<CsvImportResultResponse>> importTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CsvImportRequest request) {
        Long userId = getUserId(userDetails);
        CsvImportResultResponse response = csvImportService.executeImport(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    private Long getUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getId();
    }
}
