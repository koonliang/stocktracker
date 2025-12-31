package com.stocktracker.service;

import com.stocktracker.client.YahooFinanceClient;
import com.stocktracker.client.dto.StockQuote;
import com.stocktracker.entity.Holding;
import com.stocktracker.entity.Transaction;
import com.stocktracker.entity.TransactionType;
import com.stocktracker.entity.User;
import com.stocktracker.repository.HoldingRepository;
import com.stocktracker.repository.TransactionRepository;
import com.stocktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldingRecalculationService {

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final YahooFinanceClient yahooFinanceClient;

    /**
     * Recalculate holding for a specific symbol based on all transactions.
     * Uses FIFO (First In, First Out) method for cost basis calculation.
     */
    @Transactional
    public void recalculateHolding(Long userId, String symbol) {
        log.debug("Recalculating holding for user {} symbol {}", userId, symbol);

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndSymbolOrderByTransactionDateAsc(userId, symbol);

        if (transactions.isEmpty()) {
            // No transactions for this symbol - remove holding if exists
            holdingRepository.findByUserIdAndSymbol(userId, symbol)
                    .ifPresent(holding -> {
                        holdingRepository.delete(holding);
                        log.info("Deleted holding for user {} symbol {} (no transactions)", userId, symbol);
                    });
            return;
        }

        // Calculate net shares and weighted average cost
        BigDecimal totalShares = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        String companyName = transactions.get(0).getCompanyName();

        for (Transaction tx : transactions) {
            if (tx.getType() == TransactionType.BUY) {
                totalCost = totalCost.add(tx.getShares().multiply(tx.getPricePerShare()));
                totalShares = totalShares.add(tx.getShares());
            } else { // SELL
                // For FIFO, reduce cost proportionally
                if (totalShares.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal avgCostAtSale = totalCost.divide(totalShares, 4, RoundingMode.HALF_UP);
                    BigDecimal costReduction = tx.getShares().multiply(avgCostAtSale);
                    totalCost = totalCost.subtract(costReduction);
                    totalShares = totalShares.subtract(tx.getShares());
                }
            }
            // Keep latest company name
            companyName = tx.getCompanyName();
        }

        // If net shares is zero or negative, delete holding
        if (totalShares.compareTo(BigDecimal.ZERO) <= 0) {
            holdingRepository.findByUserIdAndSymbol(userId, symbol)
                    .ifPresent(holding -> {
                        holdingRepository.delete(holding);
                        log.info("Deleted holding for user {} symbol {} (zero shares)", userId, symbol);
                    });
            return;
        }

        // Calculate weighted average cost
        BigDecimal averageCost = totalCost.divide(totalShares, 2, RoundingMode.HALF_UP);

        // Update or create holding
        Optional<Holding> existingHolding = holdingRepository.findByUserIdAndSymbol(userId, symbol);

        if (existingHolding.isPresent()) {
            Holding holding = existingHolding.get();
            holding.setShares(totalShares);
            holding.setAverageCost(averageCost);
            holding.setCompanyName(companyName);
            holdingRepository.save(holding);
            log.info("Updated holding for user {} symbol {}: {} shares @ ${}",
                    userId, symbol, totalShares, averageCost);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Holding holding = Holding.builder()
                    .user(user)
                    .symbol(symbol)
                    .companyName(companyName)
                    .shares(totalShares)
                    .averageCost(averageCost)
                    .build();
            holdingRepository.save(holding);
            log.info("Created holding for user {} symbol {}: {} shares @ ${}",
                    userId, symbol, totalShares, averageCost);
        }
    }

    /**
     * Recalculate all holdings for a user.
     * Useful for data migration or full recalculation.
     */
    @Transactional
    public void recalculateAllHoldings(Long userId) {
        log.info("Recalculating all holdings for user {}", userId);

        // Get all symbols this user has transacted
        List<String> symbols = transactionRepository.findDistinctSymbolsByUserId(userId);

        // Recalculate each symbol
        for (String symbol : symbols) {
            recalculateHolding(userId, symbol);
        }

        log.info("Completed recalculation of {} symbols for user {}", symbols.size(), userId);
    }
}
