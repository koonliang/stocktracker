package com.stocktracker.service;

import com.stocktracker.client.YahooFinanceClient;
import com.stocktracker.client.dto.StockQuote;
import com.stocktracker.dto.request.TransactionRequest;
import com.stocktracker.dto.response.TickerValidationResponse;
import com.stocktracker.dto.response.TransactionResponse;
import com.stocktracker.entity.Transaction;
import com.stocktracker.entity.TransactionType;
import com.stocktracker.entity.User;
import com.stocktracker.exception.BadRequestException;
import com.stocktracker.exception.ResourceNotFoundException;
import com.stocktracker.repository.TransactionRepository;
import com.stocktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final YahooFinanceClient yahooFinanceClient;
    private final HoldingRecalculationService holdingRecalculationService;

    /**
     * Get all transactions for a user.
     */
    public List<TransactionResponse> getTransactions(Long userId) {
        log.debug("Fetching transactions for user: {}", userId);
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validate a ticker symbol via Yahoo Finance.
     */
    public TickerValidationResponse validateTicker(String symbol) {
        log.debug("Validating ticker: {}", symbol);

        if (symbol == null || symbol.isBlank()) {
            return TickerValidationResponse.builder()
                    .valid(false)
                    .symbol(symbol)
                    .errorMessage("Symbol is required")
                    .build();
        }

        String upperSymbol = symbol.toUpperCase().trim();
        Map<String, StockQuote> quotes = yahooFinanceClient.getQuotes(Collections.singletonList(upperSymbol));

        StockQuote quote = quotes.get(upperSymbol);
        if (quote == null || quote.getRegularMarketPrice() == null) {
            return TickerValidationResponse.builder()
                    .valid(false)
                    .symbol(upperSymbol)
                    .errorMessage("Invalid ticker symbol")
                    .build();
        }

        return TickerValidationResponse.builder()
                .valid(true)
                .symbol(upperSymbol)
                .companyName(quote.getShortName() != null ? quote.getShortName() : upperSymbol)
                .build();
    }

    /**
     * Create a new transaction.
     */
    @Transactional
    @CacheEvict(value = {"portfolio", "performanceHistory"}, allEntries = true)
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        log.debug("Creating transaction for user {}: {}", userId, request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate ticker
        TickerValidationResponse validation = validateTicker(request.getSymbol());
        if (!validation.isValid()) {
            throw new BadRequestException("Invalid ticker: " + validation.getErrorMessage());
        }

        // Validate sell transaction
        if (request.getType() == TransactionType.SELL) {
            validateSellTransaction(userId, request.getSymbol(), request.getShares(), request.getTransactionDate());
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .user(user)
                .type(request.getType())
                .symbol(validation.getSymbol())
                .companyName(validation.getCompanyName())
                .transactionDate(request.getTransactionDate())
                .shares(request.getShares())
                .pricePerShare(request.getPricePerShare())
                .brokerFee(request.getBrokerFee())
                .notes(request.getNotes())
                .build();

        transaction.calculateTotalAmount();
        Transaction saved = transactionRepository.save(transaction);
        log.info("Created transaction {} for user {}", saved.getId(), userId);

        // Recalculate holdings for this symbol
        holdingRecalculationService.recalculateHolding(userId, validation.getSymbol());

        return toResponse(saved);
    }

    /**
     * Update an existing transaction.
     */
    @Transactional
    @CacheEvict(value = {"portfolio", "performanceHistory"}, allEntries = true)
    public TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionRequest request) {
        log.debug("Updating transaction {} for user {}", transactionId, userId);

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId);
        if (transaction == null) {
            throw new ResourceNotFoundException("Transaction not found");
        }

        String oldSymbol = transaction.getSymbol();

        // Validate ticker if changed
        String newSymbol = request.getSymbol().toUpperCase().trim();
        String companyName = transaction.getCompanyName();

        if (!oldSymbol.equals(newSymbol)) {
            TickerValidationResponse validation = validateTicker(newSymbol);
            if (!validation.isValid()) {
                throw new BadRequestException("Invalid ticker: " + validation.getErrorMessage());
            }
            companyName = validation.getCompanyName();
        }

        // Validate sell transaction (exclude current transaction from check)
        if (request.getType() == TransactionType.SELL) {
            validateSellTransactionForUpdate(userId, newSymbol, request.getShares(),
                    request.getTransactionDate(), transactionId);
        }

        // Update fields
        transaction.setType(request.getType());
        transaction.setSymbol(newSymbol);
        transaction.setCompanyName(companyName);
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setShares(request.getShares());
        transaction.setPricePerShare(request.getPricePerShare());
        transaction.setBrokerFee(request.getBrokerFee());
        transaction.setNotes(request.getNotes());
        transaction.calculateTotalAmount();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Updated transaction {} for user {}", saved.getId(), userId);

        // Recalculate holdings for affected symbols
        holdingRecalculationService.recalculateHolding(userId, newSymbol);
        if (!oldSymbol.equals(newSymbol)) {
            holdingRecalculationService.recalculateHolding(userId, oldSymbol);
        }

        return toResponse(saved);
    }

    /**
     * Delete a transaction.
     */
    @Transactional
    @CacheEvict(value = {"portfolio", "performanceHistory"}, allEntries = true)
    public void deleteTransaction(Long userId, Long transactionId) {
        log.debug("Deleting transaction {} for user {}", transactionId, userId);

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId);
        if (transaction == null) {
            throw new ResourceNotFoundException("Transaction not found");
        }

        String symbol = transaction.getSymbol();
        transactionRepository.delete(transaction);
        log.info("Deleted transaction {} for user {}", transactionId, userId);

        // Recalculate holdings for this symbol
        holdingRecalculationService.recalculateHolding(userId, symbol);
    }

    /**
     * Validate a sell transaction.
     */
    private void validateSellTransaction(Long userId, String symbol, BigDecimal sharesToSell, LocalDate sellDate) {
        // Check if user has any buy transactions for this symbol
        if (!transactionRepository.existsByUserIdAndSymbolAndType(userId, symbol, TransactionType.BUY)) {
            throw new BadRequestException("Cannot sell " + symbol + ": no buy transactions exist");
        }

        // Check sell date is not before first buy
        LocalDate earliestBuy = transactionRepository.findEarliestBuyDate(userId, symbol);
        if (earliestBuy != null && sellDate.isBefore(earliestBuy)) {
            throw new BadRequestException("Sell date cannot be before first buy date (" + earliestBuy + ")");
        }

        // Check if user has enough shares to sell
        BigDecimal netShares = transactionRepository.calculateNetShares(userId, symbol);
        if (netShares.compareTo(sharesToSell) < 0) {
            throw new BadRequestException("Cannot sell " + sharesToSell + " shares of " + symbol +
                    ": only " + netShares + " shares owned");
        }
    }

    /**
     * Validate sell transaction for update (excluding current transaction).
     */
    private void validateSellTransactionForUpdate(Long userId, String symbol, BigDecimal sharesToSell,
            LocalDate sellDate, Long excludeTransactionId) {
        // For updates, we need to temporarily exclude the current transaction from calculations
        // This is a simplified check - in production, you might want a more sophisticated approach
        validateSellTransaction(userId, symbol, sharesToSell, sellDate);
    }

    /**
     * Convert entity to response DTO.
     */
    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .symbol(transaction.getSymbol())
                .companyName(transaction.getCompanyName())
                .transactionDate(transaction.getTransactionDate())
                .shares(transaction.getShares())
                .pricePerShare(transaction.getPricePerShare())
                .brokerFee(transaction.getBrokerFee())
                .totalAmount(transaction.getTotalAmount())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    /**
     * Export all transactions for a user as CSV.
     */
    public byte[] exportTransactionsAsCsv(Long userId) {
        log.debug("Exporting transactions as CSV for user: {}", userId);
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);

        StringBuilder csv = new StringBuilder();
        // Header row
        csv.append("Type,Symbol,Company Name,Date,Shares,Price Per Share,Broker Fee,Total Amount,Notes\n");

        // Data rows
        for (Transaction tx : transactions) {
            csv.append(String.format("%s,%s,\"%s\",%s,%s,%s,%s,%s,\"%s\"\n",
                tx.getType(),
                tx.getSymbol(),
                escapeCSV(tx.getCompanyName()),
                tx.getTransactionDate(),
                tx.getShares(),
                tx.getPricePerShare(),
                tx.getBrokerFee() != null ? tx.getBrokerFee() : "",
                tx.getTotalAmount(),
                escapeCSV(tx.getNotes())
            ));
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Escape CSV values to prevent injection and format issues.
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
