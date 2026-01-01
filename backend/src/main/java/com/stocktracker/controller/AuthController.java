package com.stocktracker.controller;

import com.stocktracker.dto.request.LoginRequest;
import com.stocktracker.dto.response.ApiResponse;
import com.stocktracker.dto.response.AuthResponse;
import com.stocktracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // For stateless JWT, client-side logout is sufficient
        // Backend logout is primarily for logging/auditing
        // Return 200 OK even if token is invalid to prevent information leakage
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
