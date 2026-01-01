package com.stocktracker.service;

import com.stocktracker.client.YahooFinanceClient;
import com.stocktracker.client.dto.StockQuote;
import com.stocktracker.dto.request.TransactionRequest;
import com.stocktracker.dto.response.TransactionResponse;
import com.stocktracker.entity.Transaction;
import com.stocktracker.entity.TransactionType;
import com.stocktracker.entity.User;
import com.stocktracker.exception.BadRequestException;
import com.stocktracker.repository.TransactionRepository;
import com.stocktracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private YahooFinanceClient yahooFinanceClient;

    @Mock
    private HoldingRecalculationService holdingRecalculationService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private StockQuote validQuote;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .build();
        testUser.setId(1L);

        validQuote = StockQuote.builder()
                .symbol("AAPL")
                .shortName("Apple Inc.")
                .regularMarketPrice(new BigDecimal("175.00"))
                .build();
    }

    @Test
    void validateTicker_ValidSymbol_ReturnsValid() {
        when(yahooFinanceClient.getQuotes(anyList()))
                .thenReturn(Map.of("AAPL", validQuote));

        var result = transactionService.validateTicker("aapl");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getCompanyName()).isEqualTo("Apple Inc.");
    }

    @Test
    void validateTicker_InvalidSymbol_ReturnsInvalid() {
        when(yahooFinanceClient.getQuotes(anyList()))
                .thenReturn(Collections.emptyMap());

        var result = transactionService.validateTicker("INVALID");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Invalid ticker symbol");
    }

    @Test
    void createTransaction_ValidBuy_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(yahooFinanceClient.getQuotes(anyList()))
                .thenReturn(Map.of("AAPL", validQuote));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction tx = inv.getArgument(0);
                    tx.setId(1L);
                    return tx;
                });

        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.BUY)
                .symbol("AAPL")
                .transactionDate(LocalDate.now())
                .shares(new BigDecimal("50"))
                .pricePerShare(new BigDecimal("142.50"))
                .build();

        TransactionResponse result = transactionService.createTransaction(1L, request);

        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getType()).isEqualTo(TransactionType.BUY);
        assertThat(result.getShares()).isEqualByComparingTo("50");
        verify(holdingRecalculationService).recalculateHolding(1L, "AAPL");
    }

    @Test
    void createTransaction_SellWithoutBuy_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(yahooFinanceClient.getQuotes(anyList()))
                .thenReturn(Map.of("AAPL", validQuote));
        when(transactionRepository.existsByUserIdAndSymbolAndType(1L, "AAPL", TransactionType.BUY))
                .thenReturn(false);

        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.SELL)
                .symbol("AAPL")
                .transactionDate(LocalDate.now())
                .shares(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("175.00"))
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no buy transactions exist");
    }

    @Test
    void createTransaction_SellMoreThanOwned_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(yahooFinanceClient.getQuotes(anyList()))
                .thenReturn(Map.of("AAPL", validQuote));
        when(transactionRepository.existsByUserIdAndSymbolAndType(1L, "AAPL", TransactionType.BUY))
                .thenReturn(true);
        when(transactionRepository.findEarliestBuyDate(1L, "AAPL"))
                .thenReturn(LocalDate.now().minusDays(30));
        when(transactionRepository.calculateNetShares(1L, "AAPL"))
                .thenReturn(new BigDecimal("10"));

        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.SELL)
                .symbol("AAPL")
                .transactionDate(LocalDate.now())
                .shares(new BigDecimal("50"))
                .pricePerShare(new BigDecimal("175.00"))
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only 10 shares owned");
    }
}
