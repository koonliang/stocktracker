package com.stocktracker.controller;

import com.stocktracker.dto.response.ApiResponse;
import com.stocktracker.dto.response.PortfolioResponse;
import com.stocktracker.entity.User;
import com.stocktracker.repository.UserRepository;
import com.stocktracker.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio management endpoints")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get user's portfolio with live prices")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        PortfolioResponse portfolio = portfolioService.getPortfolio(userId);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @GetMapping("/refresh")
    @Operation(summary = "Force refresh prices (bypasses cache)")
    public ResponseEntity<ApiResponse<PortfolioResponse>> refreshPortfolio(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);

        // Evict cache for this user
        evictPortfolioCache(userId);

        PortfolioResponse portfolio = portfolioService.getPortfolio(userId);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @CacheEvict(value = "portfolio", key = "#userId")
    public void evictPortfolioCache(Long userId) {
        // This method is just for cache eviction
    }

    private Long getUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getId();
    }
}
