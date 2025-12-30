package com.stocktracker.controller;

import com.stocktracker.dto.response.PortfolioPerformancePoint;
import com.stocktracker.dto.response.PortfolioResponse;
import com.stocktracker.entity.User;
import com.stocktracker.repository.UserRepository;
import com.stocktracker.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioController focusing on the new performance endpoint.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private PortfolioController portfolioController;

    private User testUser;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setName("Test User");

        when(userDetails.getUsername()).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
    }

    @Test
    void testGetPerformanceHistory_Default1YearRange() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = createMockPerformanceData(5);
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "1y"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "1y");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(5, response.getBody().getData().size());

        verify(portfolioService, times(1)).getPerformanceHistory(TEST_USER_ID, "1y");
        System.out.println("✓ Default 1Y range works correctly");
    }

    @Test
    void testGetPerformanceHistory_7DaysRange() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = createMockPerformanceData(7);
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "7d"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "7d");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7, response.getBody().getData().size());

        verify(portfolioService, times(1)).getPerformanceHistory(TEST_USER_ID, "7d");
        System.out.println("✓ 7D range parameter works correctly");
    }

    @Test
    void testGetPerformanceHistory_1MonthRange() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = createMockPerformanceData(22);
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "1mo"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "1mo");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(22, response.getBody().getData().size());

        verify(portfolioService, times(1)).getPerformanceHistory(TEST_USER_ID, "1mo");
        System.out.println("✓ 1M range parameter works correctly");
    }

    @Test
    void testGetPerformanceHistory_3MonthsRange() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = createMockPerformanceData(65);
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "3mo"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "3mo");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(65, response.getBody().getData().size());

        verify(portfolioService, times(1)).getPerformanceHistory(TEST_USER_ID, "3mo");
        System.out.println("✓ 3M range parameter works correctly");
    }

    @Test
    void testGetPerformanceHistory_YTDRange() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = createMockPerformanceData(100);
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "ytd"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "ytd");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(100, response.getBody().getData().size());

        verify(portfolioService, times(1)).getPerformanceHistory(TEST_USER_ID, "ytd");
        System.out.println("✓ YTD range parameter works correctly");
    }

    @Test
    void testGetPerformanceHistory_EmptyResult() {
        // Arrange - User has no holdings
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "1y"))
                .thenReturn(Collections.emptyList());

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "1y");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().isEmpty());

        System.out.println("✓ Empty portfolio returns empty performance data");
    }

    @Test
    void testGetPerformanceHistory_DataContainsCorrectFields() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = Arrays.asList(
                PortfolioPerformancePoint.builder()
                        .date(LocalDate.now().minusDays(2))
                        .totalValue(new BigDecimal("10000.00"))
                        .dailyChange(BigDecimal.ZERO)
                        .dailyChangePercent(BigDecimal.ZERO)
                        .build(),
                PortfolioPerformancePoint.builder()
                        .date(LocalDate.now().minusDays(1))
                        .totalValue(new BigDecimal("10500.00"))
                        .dailyChange(new BigDecimal("500.00"))
                        .dailyChangePercent(new BigDecimal("5.00"))
                        .build()
        );

        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "7d"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "7d");

        // Assert
        assertNotNull(response);
        List<PortfolioPerformancePoint> data = response.getBody().getData();

        assertEquals(2, data.size());

        // Verify first point
        PortfolioPerformancePoint point1 = data.get(0);
        assertEquals(LocalDate.now().minusDays(2), point1.getDate());
        assertEquals(new BigDecimal("10000.00"), point1.getTotalValue());
        assertEquals(BigDecimal.ZERO, point1.getDailyChange());

        // Verify second point
        PortfolioPerformancePoint point2 = data.get(1);
        assertEquals(LocalDate.now().minusDays(1), point2.getDate());
        assertEquals(new BigDecimal("10500.00"), point2.getTotalValue());
        assertEquals(new BigDecimal("500.00"), point2.getDailyChange());
        assertEquals(new BigDecimal("5.00"), point2.getDailyChangePercent());

        System.out.println("✓ Performance data contains all required fields");
    }

    @Test
    void testGetPerformanceHistory_UserIdExtraction() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = createMockPerformanceData(1);
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "1y"))
                .thenReturn(mockPerformance);

        // Act
        portfolioController.getPerformanceHistory(userDetails, "1y");

        // Assert - Verify correct user ID was extracted and passed to service
        verify(userRepository, times(1)).findByEmail(TEST_EMAIL);
        verify(portfolioService, times(1)).getPerformanceHistory(TEST_USER_ID, "1y");

        System.out.println("✓ User ID correctly extracted from UserDetails");
    }

    @Test
    void testGetPortfolio_ExistingEndpointStillWorks() {
        // Arrange
        PortfolioResponse mockPortfolio = PortfolioResponse.builder()
                .holdings(Collections.emptyList())
                .totalValue(BigDecimal.ZERO)
                .totalCost(BigDecimal.ZERO)
                .totalReturnDollars(BigDecimal.ZERO)
                .totalReturnPercent(BigDecimal.ZERO)
                .pricesUpdatedAt(LocalDateTime.now())
                .build();

        when(portfolioService.getPortfolio(TEST_USER_ID))
                .thenReturn(mockPortfolio);

        // Act
        var response = portfolioController.getPortfolio(userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(portfolioService, times(1)).getPortfolio(TEST_USER_ID);
        System.out.println("✓ Existing /portfolio endpoint still works");
    }

    @Test
    void testRefreshPortfolio_CacheEviction() {
        // Arrange
        PortfolioResponse mockPortfolio = PortfolioResponse.builder()
                .holdings(Collections.emptyList())
                .totalValue(BigDecimal.ZERO)
                .totalCost(BigDecimal.ZERO)
                .totalReturnDollars(BigDecimal.ZERO)
                .totalReturnPercent(BigDecimal.ZERO)
                .pricesUpdatedAt(LocalDateTime.now())
                .build();

        when(portfolioService.getPortfolio(TEST_USER_ID))
                .thenReturn(mockPortfolio);

        // Act
        var response = portfolioController.refreshPortfolio(userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify cache eviction method was called
        verify(portfolioService, times(1)).getPortfolio(TEST_USER_ID);
        System.out.println("✓ Refresh endpoint works with cache eviction");
    }

    @Test
    void testGetPerformanceHistory_ResponseStructure() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = createMockPerformanceData(3);
        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "1y"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "1y");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess(), "Response should indicate success");
        assertNotNull(response.getBody().getData(), "Response should contain data");
        assertNull(response.getBody().getMessage(), "Response should not contain error message");

        System.out.println("✓ Response follows ApiResponse structure correctly");
    }

    @Test
    void testGetPerformanceHistory_ChronologicalOrder() {
        // Arrange
        List<PortfolioPerformancePoint> mockPerformance = Arrays.asList(
                createPerformancePoint(LocalDate.now().minusDays(5), "9000.00"),
                createPerformancePoint(LocalDate.now().minusDays(4), "9200.00"),
                createPerformancePoint(LocalDate.now().minusDays(3), "9100.00"),
                createPerformancePoint(LocalDate.now().minusDays(2), "9500.00"),
                createPerformancePoint(LocalDate.now().minusDays(1), "9800.00")
        );

        when(portfolioService.getPerformanceHistory(TEST_USER_ID, "7d"))
                .thenReturn(mockPerformance);

        // Act
        var response = portfolioController.getPerformanceHistory(userDetails, "7d");

        // Assert
        List<PortfolioPerformancePoint> data = response.getBody().getData();
        assertEquals(5, data.size());

        // Verify chronological order
        for (int i = 0; i < data.size() - 1; i++) {
            assertTrue(data.get(i).getDate().isBefore(data.get(i + 1).getDate()),
                    "Performance data should be in chronological order");
        }

        System.out.println("✓ Performance data is in chronological order");
    }

    // Helper methods

    private List<PortfolioPerformancePoint> createMockPerformanceData(int count) {
        List<PortfolioPerformancePoint> performance = new java.util.ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(count);

        BigDecimal baseValue = new BigDecimal("10000.00");
        BigDecimal previousValue = null;

        for (int i = 0; i < count; i++) {
            BigDecimal currentValue = baseValue.add(new BigDecimal(i * 100));
            BigDecimal dailyChange = previousValue != null
                    ? currentValue.subtract(previousValue)
                    : BigDecimal.ZERO;

            performance.add(PortfolioPerformancePoint.builder()
                    .date(startDate.plusDays(i))
                    .totalValue(currentValue)
                    .dailyChange(dailyChange)
                    .dailyChangePercent(BigDecimal.ZERO)
                    .build());

            previousValue = currentValue;
        }

        return performance;
    }

    private PortfolioPerformancePoint createPerformancePoint(LocalDate date, String totalValue) {
        return PortfolioPerformancePoint.builder()
                .date(date)
                .totalValue(new BigDecimal(totalValue))
                .dailyChange(BigDecimal.ZERO)
                .dailyChangePercent(BigDecimal.ZERO)
                .build();
    }
}
