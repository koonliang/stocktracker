package com.stocktracker.controller;

import com.stocktracker.dto.request.LoginRequest;
import com.stocktracker.dto.request.SignupRequest;
import com.stocktracker.dto.response.ApiResponse;
import com.stocktracker.dto.response.AuthResponse;
import com.stocktracker.entity.User;
import com.stocktracker.security.JwtTokenProvider;
import com.stocktracker.service.AuthService;
import com.stocktracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody SignupRequest request) {
        User user = userService.registerUser(request);
        
        // Auto-login after registration
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);
        
        AuthResponse response = AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .userId(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .build();
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // For stateless JWT, client-side logout is sufficient
        // Backend logout is primarily for logging/auditing
        // Return 200 OK even if token is invalid to prevent information leakage
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/demo-login")
    public ResponseEntity<ApiResponse<AuthResponse>> demoLogin() {
        AuthResponse response = authService.demoLogin();
        return ResponseEntity.ok(ApiResponse.success("Demo account created", response));
    }
}
