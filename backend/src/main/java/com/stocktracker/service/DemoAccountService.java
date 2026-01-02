package com.stocktracker.service;

import com.stocktracker.entity.Transaction;
import com.stocktracker.entity.TransactionType;
import com.stocktracker.entity.User;
import com.stocktracker.repository.TransactionRepository;
import com.stocktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoAccountService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final HoldingRecalculationService holdingRecalculationService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a demo account with seeded portfolio data.
     * Each demo login creates a fresh user with sample transactions.
     */
    @Transactional
    public User createDemoAccount() {
        // Generate unique demo email
        String demoEmail = "demo-" + UUID.randomUUID() + "@stocktracker.demo";

        // Create demo user
        User demoUser = User.builder()
                .name("Demo User")
                .email(demoEmail)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .enabled(true)
                .isDemoAccount(true)
                .role(User.Role.USER)
                .build();

        demoUser = userRepository.save(demoUser);
        log.info("Created demo account with email: {}", demoEmail);

        // Seed demo stock transactions
        seedDemoTransactions(demoUser);

        // Recalculate all holdings for the demo user
        holdingRecalculationService.recalculateAllHoldings(demoUser.getId());

        return demoUser;
    }

    private void seedDemoTransactions(User user) {
        LocalDate baseDate = LocalDate.now().minusDays(90); // Start 90 days ago

        // AAPL - Apple Inc.
        createTransaction(user, TransactionType.BUY, "AAPL", "Apple Inc.",
                baseDate, new BigDecimal("60"), new BigDecimal("142.50"));
        createTransaction(user, TransactionType.SELL, "AAPL", "Apple Inc.",
                baseDate.plusDays(30), new BigDecimal("10"), new BigDecimal("150.00"));

        // MSFT - Microsoft Corporation
        createTransaction(user, TransactionType.BUY, "MSFT", "Microsoft Corporation",
                baseDate.plusDays(5), new BigDecimal("30"), new BigDecimal("285.00"));
        createTransaction(user, TransactionType.SELL, "MSFT", "Microsoft Corporation",
                baseDate.plusDays(35), new BigDecimal("5"), new BigDecimal("320.00"));

        // GOOGL - Alphabet Inc.
        createTransaction(user, TransactionType.BUY, "GOOGL", "Alphabet Inc.",
                baseDate.plusDays(10), new BigDecimal("10"), new BigDecimal("125.30"));

        // TSLA - Tesla, Inc.
        createTransaction(user, TransactionType.BUY, "TSLA", "Tesla, Inc.",
                baseDate.plusDays(15), new BigDecimal("20"), new BigDecimal("248.00"));
        createTransaction(user, TransactionType.SELL, "TSLA", "Tesla, Inc.",
                baseDate.plusDays(45), new BigDecimal("5"), new BigDecimal("265.00"));

        // NVDA - NVIDIA Corporation
        createTransaction(user, TransactionType.BUY, "NVDA", "NVIDIA Corporation",
                baseDate.plusDays(20), new BigDecimal("20"), new BigDecimal("450.00"));

        // AMZN - Amazon.com, Inc.
        createTransaction(user, TransactionType.BUY, "AMZN", "Amazon.com, Inc.",
                baseDate.plusDays(25), new BigDecimal("40"), new BigDecimal("135.00"));
        createTransaction(user, TransactionType.SELL, "AMZN", "Amazon.com, Inc.",
                baseDate.plusDays(55), new BigDecimal("10"), new BigDecimal("145.00"));

        log.info("Seeded {} demo transactions for user {}", 12, user.getId());
    }

    private void createTransaction(User user, TransactionType type, String symbol,
                                   String companyName, LocalDate date,
                                   BigDecimal shares, BigDecimal pricePerShare) {
        Transaction transaction = Transaction.builder()
                .user(user)
                .type(type)
                .symbol(symbol)
                .companyName(companyName)
                .transactionDate(date)
                .shares(shares)
                .pricePerShare(pricePerShare)
                .notes("Demo seed data")
                .build();

        transactionRepository.save(transaction);
    }
}
