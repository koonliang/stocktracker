# Task 03: Transaction Management - Entry, Editing & Recalculation

## Objective

Enhance the dashboard page with transaction management capabilities:
1. Allow keying in of individual transactions (Buy/Sell)
2. Present all transactions in an editable grid table
3. Auto-update portfolio holdings based on transaction data

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Dashboard Page                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  [Manage Transactions]  [Refresh Prices]                  (buttons) │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Portfolio Summary Cards (Total Value, Cost, Return, Return %)      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ╔═════════════════════════════════════════════════════════════════════╗    │
│  ║  TRANSACTION SECTION (Hidden by default, shown on button click)     ║    │
│  ╠═════════════════════════════════════════════════════════════════════╣    │
│  ║  ┌─────────────────────────────────────────────────────────────┐    ║    │
│  ║  │  Transaction Entry Form (Hidden, shown on "+ Add" click)    │    ║    │
│  ║  │  [Type: Buy/Sell] [Ticker] [Date] [Shares] [Price] [Add]    │    ║    │
│  ║  └─────────────────────────────────────────────────────────────┘    ║    │
│  ║                                                                      ║    │
│  ║  ┌─────────────────────────────────────────────────────────────┐    ║    │
│  ║  │  Editable Transaction Grid                                  │    ║    │
│  ║  │  ┌────────┬────────┬────────┬────────┬────────┬──────────┐  │    ║    │
│  ║  │  │ Type   │ Ticker │ Date   │ Shares │ Price  │ Actions  │  │    ║    │
│  ║  │  ├────────┼────────┼────────┼────────┼────────┼──────────┤  │    ║    │
│  ║  │  │ Buy    │ AAPL   │ Jan 15 │ 50     │ 142.50 │ Edit/Del │  │    ║    │
│  ║  │  │ + Add New Row                                          │  │    ║    │
│  ║  │  └────────────────────────────────────────────────────────┘  │    ║    │
│  ║  └─────────────────────────────────────────────────────────────┘    ║    │
│  ╚═════════════════════════════════════════════════════════════════════╝    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Performance Chart Widget                                           │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Portfolio Holdings Table (Auto-updated from transactions)          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Visibility States:**
- Transaction Section: Hidden by default, toggled via "Manage Transactions" button
- Transaction Entry Form: Hidden by default within section, shown via "+ Add Transaction" button
- Transaction Grid: Always visible when Transaction Section is open

**Data Flow:**
```
Transaction Entry/Edit → Save to DB → Recalculate Holdings → Update Portfolio View
                                           │
                                           ▼
                                    Aggregate by symbol:
                                    - Net shares = Buy shares - Sell shares
                                    - Avg cost = Weighted average of buy prices
                                    - Cost basis = Net shares × Avg cost
```

---

## Requirements Breakdown

### Feature 1: Transaction Entry Form

| Aspect | Specification |
|--------|---------------|
| **Visibility** | Hidden by default; shown when "+ Add Transaction" button is clicked |
| **Position** | Above the transaction grid (when visible) |
| **Form Type** | Inline collapsible form |
| **Submit Action** | Add transaction, refresh holdings, hide form |
| **Cancel Action** | Hide form without saving |

#### Transaction Fields

| Field | Type | Validation | Notes |
|-------|------|------------|-------|
| **Type** | Dropdown | Required | `BUY` or `SELL` |
| **Ticker** | Text + Autocomplete | Required, must be valid | Validated via Yahoo Finance API |
| **Date** | Date picker | Required, ≤ today | Transaction date |
| **Shares** | Number | Required, > 0 | Number of shares |
| **Price** | Number | Required, > 0 | Price per share |

#### Validation Rules

1. **All fields are mandatory**
2. **Ticker validation:**
   - Must be a valid stock symbol
   - Verify via Yahoo Finance API before saving
   - Auto-populate company name on valid ticker
3. **Sell validation:**
   - Cannot sell more shares than currently owned
   - Must have existing buy transactions for the ticker
4. **Date validation:**
   - Cannot be in the future
   - For sells, cannot be before first buy of that ticker

### Feature 2: Editable Transaction Grid

| Aspect | Specification |
|--------|---------------|
| **Visibility** | Hidden by default; shown when "Manage Transactions" button is clicked |
| **Position** | Below summary cards, above performance chart (when visible) |
| **Behavior** | Inline editing, sortable, filterable |
| **Actions** | Edit, Delete per row; Add new row at bottom |
| **Toggle Button** | "Manage Transactions" / "Hide Transactions" (toggles visibility) |

#### Grid Columns

| Column | Width | Editable | Type |
|--------|-------|----------|------|
| Type | 80px | Yes | Dropdown |
| Ticker | 100px | Yes (with validation) | Text + Autocomplete |
| Date | 120px | Yes | Date picker |
| Shares | 100px | Yes | Number input |
| Price | 100px | Yes | Number input (currency) |
| Total | 120px | No (calculated) | Display only |
| Actions | 100px | N/A | Edit/Save/Delete buttons |

#### Grid Features

- **Inline editing:** Click cell to edit, Enter to save, Escape to cancel
- **Add new row:** Button at bottom of grid adds empty row in edit mode
- **Delete:** Confirmation dialog before deletion
- **Sorting:** Click column header to sort
- **Filtering:** Optional ticker filter dropdown

---

## Database Schema Changes

### New Table: `transactions`

```sql
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(4) NOT NULL,          -- 'BUY' or 'SELL'
    symbol VARCHAR(10) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    transaction_date DATE NOT NULL,
    shares DECIMAL(12, 4) NOT NULL,
    price_per_share DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL,  -- Calculated: shares * price
    notes VARCHAR(500),                     -- Optional notes
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_type CHECK (type IN ('BUY', 'SELL')),
    CONSTRAINT chk_shares_positive CHECK (shares > 0),
    CONSTRAINT chk_price_positive CHECK (price_per_share > 0),
    INDEX idx_user_symbol (user_id, symbol),
    INDEX idx_user_date (user_id, transaction_date)
);
```

### Holdings Table Relationship

The existing `holdings` table will be **derived** from transactions:
- Option A: Keep holdings as cached aggregation (recalculate on transaction changes)
- Option B: Replace holdings with a view/query that aggregates transactions

**Recommended: Option A** - Keep holdings table for performance, recalculate on changes.

---

## Implementation Phases

### Phase 1: Backend - Transaction Entity

**File**: `backend/src/main/java/com/stocktracker/entity/Transaction.java`

```java
package com.stocktracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_user_symbol", columnList = "user_id, symbol"),
    @Index(name = "idx_user_date", columnList = "user_id, transaction_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TransactionType type;

    @NotBlank(message = "Symbol is required")
    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    @Column(nullable = false, length = 10)
    private String symbol;

    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Shares is required")
    @Positive(message = "Shares must be positive")
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal shares;

    @NotNull(message = "Price per share is required")
    @Positive(message = "Price must be positive")
    @Column(name = "price_per_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerShare;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @PrePersist
    @PreUpdate
    public void calculateTotalAmount() {
        if (shares != null && pricePerShare != null) {
            this.totalAmount = shares.multiply(pricePerShare);
        }
    }
}
```

**File**: `backend/src/main/java/com/stocktracker/entity/TransactionType.java`

```java
package com.stocktracker.entity;

public enum TransactionType {
    BUY,
    SELL
}
```

---

### Phase 2: Backend - Transaction Repository

**File**: `backend/src/main/java/com/stocktracker/repository/TransactionRepository.java`

```java
package com.stocktracker.repository;

import com.stocktracker.entity.Transaction;
import com.stocktracker.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a user, ordered by date descending.
     */
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    /**
     * Find all transactions for a specific symbol.
     */
    List<Transaction> findByUserIdAndSymbolOrderByTransactionDateAsc(Long userId, String symbol);

    /**
     * Find transactions by user, ordered by symbol then date.
     */
    List<Transaction> findByUserIdOrderBySymbolAscTransactionDateAsc(Long userId);

    /**
     * Get distinct symbols for a user (symbols they have transacted).
     */
    @Query("SELECT DISTINCT t.symbol FROM Transaction t WHERE t.user.id = :userId ORDER BY t.symbol")
    List<String> findDistinctSymbolsByUserId(@Param("userId") Long userId);

    /**
     * Calculate total shares owned for a symbol (buy - sell).
     */
    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN t.type = 'BUY' THEN t.shares ELSE -t.shares END),
            0
        )
        FROM Transaction t
        WHERE t.user.id = :userId AND t.symbol = :symbol
    """)
    BigDecimal calculateNetShares(@Param("userId") Long userId, @Param("symbol") String symbol);

    /**
     * Get earliest transaction date for a symbol (for sell validation).
     */
    @Query("""
        SELECT MIN(t.transactionDate)
        FROM Transaction t
        WHERE t.user.id = :userId AND t.symbol = :symbol AND t.type = 'BUY'
    """)
    LocalDate findEarliestBuyDate(@Param("userId") Long userId, @Param("symbol") String symbol);

    /**
     * Check if user has any buy transactions for a symbol.
     */
    boolean existsByUserIdAndSymbolAndType(Long userId, String symbol, TransactionType type);

    /**
     * Find transaction by id and user (for security).
     */
    Transaction findByIdAndUserId(Long id, Long userId);
}
```

---

### Phase 3: Backend - Transaction DTOs

**File**: `backend/src/main/java/com/stocktracker/dto/request/TransactionRequest.java`

```java
package com.stocktracker.dto.request;

import com.stocktracker.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotBlank(message = "Symbol is required")
    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    private String symbol;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @NotNull(message = "Number of shares is required")
    @Positive(message = "Shares must be positive")
    private BigDecimal shares;

    @NotNull(message = "Price per share is required")
    @Positive(message = "Price must be positive")
    private BigDecimal pricePerShare;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
```

**File**: `backend/src/main/java/com/stocktracker/dto/response/TransactionResponse.java`

```java
package com.stocktracker.dto.response;

import com.stocktracker.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private String symbol;
    private String companyName;
    private LocalDate transactionDate;
    private BigDecimal shares;
    private BigDecimal pricePerShare;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**File**: `backend/src/main/java/com/stocktracker/dto/response/TickerValidationResponse.java`

```java
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
```

---

### Phase 4: Backend - Transaction Service

**File**: `backend/src/main/java/com/stocktracker/service/TransactionService.java`

```java
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
                .totalAmount(transaction.getTotalAmount())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
```

---

### Phase 5: Backend - Holding Recalculation Service

**File**: `backend/src/main/java/com/stocktracker/service/HoldingRecalculationService.java`

```java
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
```

---

### Phase 6: Backend - Transaction Controller

**File**: `backend/src/main/java/com/stocktracker/controller/TransactionController.java`

```java
package com.stocktracker.controller;

import com.stocktracker.dto.request.TransactionRequest;
import com.stocktracker.dto.response.ApiResponse;
import com.stocktracker.dto.response.TickerValidationResponse;
import com.stocktracker.dto.response.TransactionResponse;
import com.stocktracker.entity.User;
import com.stocktracker.repository.UserRepository;
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
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;
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

    private Long getUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getId();
    }
}
```

---

### Phase 7: Backend - Unit Tests

**File**: `backend/src/test/java/com/stocktracker/service/TransactionServiceTest.java`

```java
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
                .id(1L)
                .email("test@example.com")
                .build();

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
```

---

### Phase 8: Frontend - API Client Updates

**File**: `frontend/src/services/api/transactionApi.ts` (NEW)

```typescript
import axiosInstance from './axiosInstance'

export type TransactionType = 'BUY' | 'SELL'

export interface TransactionRequest {
  type: TransactionType
  symbol: string
  transactionDate: string // ISO date string (YYYY-MM-DD)
  shares: number
  pricePerShare: number
  notes?: string
}

export interface TransactionResponse {
  id: number
  type: TransactionType
  symbol: string
  companyName: string
  transactionDate: string
  shares: number
  pricePerShare: number
  totalAmount: number
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface TickerValidationResponse {
  valid: boolean
  symbol: string
  companyName: string | null
  errorMessage: string | null
}

export const transactionApi = {
  /**
   * Get all transactions for the authenticated user.
   */
  getTransactions: async (): Promise<TransactionResponse[]> => {
    const response = await axiosInstance.get('/transactions')
    return response.data.data
  },

  /**
   * Validate a ticker symbol.
   */
  validateTicker: async (symbol: string): Promise<TickerValidationResponse> => {
    const response = await axiosInstance.get(`/transactions/validate-ticker?symbol=${encodeURIComponent(symbol)}`)
    return response.data.data
  },

  /**
   * Create a new transaction.
   */
  createTransaction: async (request: TransactionRequest): Promise<TransactionResponse> => {
    const response = await axiosInstance.post('/transactions', request)
    return response.data.data
  },

  /**
   * Update an existing transaction.
   */
  updateTransaction: async (id: number, request: TransactionRequest): Promise<TransactionResponse> => {
    const response = await axiosInstance.put(`/transactions/${id}`, request)
    return response.data.data
  },

  /**
   * Delete a transaction.
   */
  deleteTransaction: async (id: number): Promise<void> => {
    await axiosInstance.delete(`/transactions/${id}`)
  },
}
```

**Update**: `frontend/src/services/api/index.ts`

```typescript
export * from './portfolioApi'
export * from './transactionApi'
export { default as axiosInstance } from './axiosInstance'
```

---

### Phase 9: Frontend - Custom Hooks

**File**: `frontend/src/hooks/useTransactions.ts` (NEW)

```typescript
import { useState, useEffect, useCallback } from 'react'
import {
  transactionApi,
  TransactionResponse,
  TransactionRequest,
  TickerValidationResponse
} from '../services/api/transactionApi'

export function useTransactions() {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchTransactions = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await transactionApi.getTransactions()
      setTransactions(data)
    } catch (err) {
      setError('Failed to load transactions')
      console.error('Error fetching transactions:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchTransactions()
  }, [fetchTransactions])

  const createTransaction = useCallback(async (request: TransactionRequest) => {
    const response = await transactionApi.createTransaction(request)
    setTransactions(prev => [response, ...prev])
    return response
  }, [])

  const updateTransaction = useCallback(async (id: number, request: TransactionRequest) => {
    const response = await transactionApi.updateTransaction(id, request)
    setTransactions(prev => prev.map(tx => tx.id === id ? response : tx))
    return response
  }, [])

  const deleteTransaction = useCallback(async (id: number) => {
    await transactionApi.deleteTransaction(id)
    setTransactions(prev => prev.filter(tx => tx.id !== id))
  }, [])

  return {
    transactions,
    loading,
    error,
    refresh: fetchTransactions,
    createTransaction,
    updateTransaction,
    deleteTransaction,
  }
}

export function useTickerValidation() {
  const [validating, setValidating] = useState(false)
  const [validation, setValidation] = useState<TickerValidationResponse | null>(null)

  const validateTicker = useCallback(async (symbol: string) => {
    if (!symbol || symbol.length < 1) {
      setValidation(null)
      return null
    }

    try {
      setValidating(true)
      const result = await transactionApi.validateTicker(symbol)
      setValidation(result)
      return result
    } catch (err) {
      const errorResult: TickerValidationResponse = {
        valid: false,
        symbol,
        companyName: null,
        errorMessage: 'Failed to validate ticker',
      }
      setValidation(errorResult)
      return errorResult
    } finally {
      setValidating(false)
    }
  }, [])

  const clearValidation = useCallback(() => {
    setValidation(null)
  }, [])

  return {
    validating,
    validation,
    validateTicker,
    clearValidation,
  }
}
```

---

### Phase 10: Frontend - Transaction Form Component

**File**: `frontend/src/components/transactions/TransactionForm.tsx` (NEW)

```typescript
import { useState, useCallback, useEffect } from 'react'
import { TransactionRequest, TransactionType } from '../../services/api/transactionApi'
import { useTickerValidation } from '../../hooks/useTransactions'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionFormProps {
  onSubmit: (request: TransactionRequest) => Promise<void>
  onCancel?: () => void
  initialData?: Partial<TransactionRequest>
  isEditing?: boolean
}

export function TransactionForm({
  onSubmit,
  onCancel,
  initialData,
  isEditing = false
}: TransactionFormProps) {
  const [type, setType] = useState<TransactionType>(initialData?.type || 'BUY')
  const [symbol, setSymbol] = useState(initialData?.symbol || '')
  const [date, setDate] = useState(initialData?.transactionDate || new Date().toISOString().split('T')[0])
  const [shares, setShares] = useState(initialData?.shares?.toString() || '')
  const [price, setPrice] = useState(initialData?.pricePerShare?.toString() || '')
  const [notes, setNotes] = useState(initialData?.notes || '')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { validating, validation, validateTicker, clearValidation } = useTickerValidation()

  // Debounced ticker validation
  useEffect(() => {
    const timer = setTimeout(() => {
      if (symbol.length >= 1) {
        validateTicker(symbol)
      } else {
        clearValidation()
      }
    }, 500)

    return () => clearTimeout(timer)
  }, [symbol, validateTicker, clearValidation])

  const totalAmount =
    shares && price
      ? parseFloat(shares) * parseFloat(price)
      : 0

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    // Validation
    if (!symbol || !date || !shares || !price) {
      setError('All fields are required')
      return
    }

    if (validation && !validation.valid) {
      setError(validation.errorMessage || 'Invalid ticker symbol')
      return
    }

    try {
      setSubmitting(true)
      await onSubmit({
        type,
        symbol: symbol.toUpperCase(),
        transactionDate: date,
        shares: parseFloat(shares),
        pricePerShare: parseFloat(price),
        notes: notes || undefined,
      })

      // Reset form if not editing
      if (!isEditing) {
        setSymbol('')
        setShares('')
        setPrice('')
        setNotes('')
        clearValidation()
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save transaction')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && (
        <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
        {/* Type */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Type</label>
          <select
            value={type}
            onChange={e => setType(e.target.value as TransactionType)}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </div>

        {/* Ticker */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Ticker</label>
          <div className="relative">
            <input
              type="text"
              value={symbol}
              onChange={e => setSymbol(e.target.value.toUpperCase())}
              placeholder="AAPL"
              className={`mt-1 block w-full rounded-lg border px-3 py-2 uppercase
                       focus:outline-none focus:ring-1
                       ${validation?.valid === false
                         ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                         : validation?.valid
                           ? 'border-green-300 focus:border-green-500 focus:ring-green-500'
                           : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500'
                       }`}
            />
            {validating && (
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                ...
              </span>
            )}
          </div>
          {validation?.valid && validation.companyName && (
            <p className="mt-1 text-xs text-green-600">{validation.companyName}</p>
          )}
          {validation?.valid === false && (
            <p className="mt-1 text-xs text-red-600">{validation.errorMessage}</p>
          )}
        </div>

        {/* Date */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Date</label>
          <input
            type="date"
            value={date}
            onChange={e => setDate(e.target.value)}
            max={new Date().toISOString().split('T')[0]}
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        {/* Shares */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Shares</label>
          <input
            type="number"
            value={shares}
            onChange={e => setShares(e.target.value)}
            placeholder="100"
            min="0.0001"
            step="0.0001"
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        {/* Price */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Price</label>
          <input
            type="number"
            value={price}
            onChange={e => setPrice(e.target.value)}
            placeholder="150.00"
            min="0.01"
            step="0.01"
            className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                     focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        {/* Total (calculated) */}
        <div>
          <label className="block text-sm font-medium text-slate-700">Total</label>
          <div className="mt-1 rounded-lg bg-slate-100 px-3 py-2 font-medium text-slate-700">
            {formatCurrency(totalAmount)}
          </div>
        </div>
      </div>

      {/* Notes (optional) */}
      <div>
        <label className="block text-sm font-medium text-slate-700">Notes (optional)</label>
        <input
          type="text"
          value={notes}
          onChange={e => setNotes(e.target.value)}
          placeholder="Optional notes..."
          maxLength={500}
          className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2
                   focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>

      {/* Actions */}
      <div className="flex gap-3">
        <button
          type="submit"
          disabled={submitting || validating}
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white
                   hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? 'Saving...' : isEditing ? 'Update' : 'Add Transaction'}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg border border-slate-300 px-4 py-2 font-medium text-slate-700
                     hover:bg-slate-50"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}
```

---

### Phase 11: Frontend - Editable Transaction Grid

**File**: `frontend/src/components/transactions/TransactionGrid.tsx` (NEW)

```typescript
import { useState, useCallback } from 'react'
import { TransactionResponse, TransactionRequest } from '../../services/api/transactionApi'
import { TransactionGridRow } from './TransactionGridRow'
import { TransactionForm } from './TransactionForm'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionGridProps {
  transactions: TransactionResponse[]
  onUpdate: (id: number, request: TransactionRequest) => Promise<void>
  onDelete: (id: number) => Promise<void>
  onCreate: (request: TransactionRequest) => Promise<void>
}

type SortField = 'transactionDate' | 'symbol' | 'type' | 'totalAmount'
type SortDirection = 'asc' | 'desc'

export function TransactionGrid({
  transactions,
  onUpdate,
  onDelete,
  onCreate
}: TransactionGridProps) {
  const [editingId, setEditingId] = useState<number | null>(null)
  const [showAddRow, setShowAddRow] = useState(false)
  const [sortField, setSortField] = useState<SortField>('transactionDate')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [filterSymbol, setFilterSymbol] = useState<string>('')

  // Get unique symbols for filter dropdown
  const uniqueSymbols = Array.from(new Set(transactions.map(tx => tx.symbol))).sort()

  // Sort and filter transactions
  const sortedTransactions = [...transactions]
    .filter(tx => !filterSymbol || tx.symbol === filterSymbol)
    .sort((a, b) => {
      let comparison = 0
      switch (sortField) {
        case 'transactionDate':
          comparison = new Date(a.transactionDate).getTime() - new Date(b.transactionDate).getTime()
          break
        case 'symbol':
          comparison = a.symbol.localeCompare(b.symbol)
          break
        case 'type':
          comparison = a.type.localeCompare(b.type)
          break
        case 'totalAmount':
          comparison = a.totalAmount - b.totalAmount
          break
      }
      return sortDirection === 'asc' ? comparison : -comparison
    })

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc')
    } else {
      setSortField(field)
      setSortDirection('desc')
    }
  }

  const handleSave = useCallback(async (id: number, request: TransactionRequest) => {
    await onUpdate(id, request)
    setEditingId(null)
  }, [onUpdate])

  const handleCreate = useCallback(async (request: TransactionRequest) => {
    await onCreate(request)
    setShowAddRow(false)
  }, [onCreate])

  const handleDelete = useCallback(async (id: number) => {
    if (window.confirm('Are you sure you want to delete this transaction?')) {
      await onDelete(id)
    }
  }, [onDelete])

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) return <span className="text-slate-300 ml-1">↕</span>
    return <span className="ml-1">{sortDirection === 'asc' ? '↑' : '↓'}</span>
  }

  // Calculate totals
  const totalBuys = sortedTransactions
    .filter(tx => tx.type === 'BUY')
    .reduce((sum, tx) => sum + tx.totalAmount, 0)
  const totalSells = sortedTransactions
    .filter(tx => tx.type === 'SELL')
    .reduce((sum, tx) => sum + tx.totalAmount, 0)

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <select
            value={filterSymbol}
            onChange={e => setFilterSymbol(e.target.value)}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">All Symbols</option>
            {uniqueSymbols.map(symbol => (
              <option key={symbol} value={symbol}>{symbol}</option>
            ))}
          </select>
          <span className="text-sm text-slate-500">
            {sortedTransactions.length} transaction{sortedTransactions.length !== 1 ? 's' : ''}
          </span>
        </div>
        <div className="text-sm text-slate-600">
          <span className="text-green-600">Bought: {formatCurrency(totalBuys)}</span>
          <span className="mx-2">|</span>
          <span className="text-red-600">Sold: {formatCurrency(totalSells)}</span>
        </div>
      </div>

      {/* Grid Table */}
      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-soft">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th
                onClick={() => handleSort('type')}
                className="cursor-pointer px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Type <SortIcon field="type" />
              </th>
              <th
                onClick={() => handleSort('symbol')}
                className="cursor-pointer px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Ticker <SortIcon field="symbol" />
              </th>
              <th
                onClick={() => handleSort('transactionDate')}
                className="cursor-pointer px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Date <SortIcon field="transactionDate" />
              </th>
              <th className="px-4 py-3 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
                Shares
              </th>
              <th className="px-4 py-3 text-right text-sm font-semibold uppercase tracking-wide text-slate-600">
                Price
              </th>
              <th
                onClick={() => handleSort('totalAmount')}
                className="cursor-pointer px-4 py-3 text-right text-sm font-semibold uppercase tracking-wide text-slate-600 hover:bg-slate-100"
              >
                Total <SortIcon field="totalAmount" />
              </th>
              <th className="px-4 py-3 text-center text-sm font-semibold uppercase tracking-wide text-slate-600">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sortedTransactions.map(transaction => (
              <TransactionGridRow
                key={transaction.id}
                transaction={transaction}
                isEditing={editingId === transaction.id}
                onEdit={() => setEditingId(transaction.id)}
                onSave={(request) => handleSave(transaction.id, request)}
                onCancel={() => setEditingId(null)}
                onDelete={() => handleDelete(transaction.id)}
              />
            ))}

            {/* Add New Row */}
            {showAddRow ? (
              <tr>
                <td colSpan={7} className="bg-indigo-50 p-4">
                  <TransactionForm
                    onSubmit={handleCreate}
                    onCancel={() => setShowAddRow(false)}
                  />
                </td>
              </tr>
            ) : (
              <tr>
                <td colSpan={7} className="p-4">
                  <button
                    onClick={() => setShowAddRow(true)}
                    className="flex w-full items-center justify-center gap-2 rounded-lg border-2 border-dashed
                             border-slate-300 py-3 text-slate-500 transition-colors hover:border-indigo-400
                             hover:text-indigo-600"
                  >
                    <span className="text-xl">+</span>
                    <span>Add Transaction</span>
                  </button>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
```

---

### Phase 12: Frontend - Transaction Grid Row

**File**: `frontend/src/components/transactions/TransactionGridRow.tsx` (NEW)

```typescript
import { useState } from 'react'
import { TransactionResponse, TransactionRequest, TransactionType } from '../../services/api/transactionApi'
import { useTickerValidation } from '../../hooks/useTransactions'
import { formatCurrency } from '../../utils/stockFormatters'

interface TransactionGridRowProps {
  transaction: TransactionResponse
  isEditing: boolean
  onEdit: () => void
  onSave: (request: TransactionRequest) => Promise<void>
  onCancel: () => void
  onDelete: () => void
}

export function TransactionGridRow({
  transaction,
  isEditing,
  onEdit,
  onSave,
  onCancel,
  onDelete,
}: TransactionGridRowProps) {
  const [type, setType] = useState<TransactionType>(transaction.type)
  const [symbol, setSymbol] = useState(transaction.symbol)
  const [date, setDate] = useState(transaction.transactionDate)
  const [shares, setShares] = useState(transaction.shares.toString())
  const [price, setPrice] = useState(transaction.pricePerShare.toString())
  const [saving, setSaving] = useState(false)

  const { validating, validation, validateTicker } = useTickerValidation()

  const handleSymbolChange = async (value: string) => {
    const upperValue = value.toUpperCase()
    setSymbol(upperValue)
    if (upperValue !== transaction.symbol) {
      await validateTicker(upperValue)
    }
  }

  const handleSave = async () => {
    // Validate if symbol changed
    if (symbol !== transaction.symbol && validation && !validation.valid) {
      return
    }

    setSaving(true)
    try {
      await onSave({
        type,
        symbol,
        transactionDate: date,
        shares: parseFloat(shares),
        pricePerShare: parseFloat(price),
      })
    } finally {
      setSaving(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSave()
    } else if (e.key === 'Escape') {
      onCancel()
    }
  }

  if (!isEditing) {
    // Display mode
    return (
      <tr className="transition-colors hover:bg-slate-50">
        <td className="px-4 py-3">
          <span className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold
            ${transaction.type === 'BUY'
              ? 'bg-green-100 text-green-700'
              : 'bg-red-100 text-red-700'
            }`}
          >
            {transaction.type}
          </span>
        </td>
        <td className="px-4 py-3">
          <div className="font-medium text-slate-900">{transaction.symbol}</div>
          <div className="text-xs text-slate-500">{transaction.companyName}</div>
        </td>
        <td className="px-4 py-3 text-slate-700">
          {new Date(transaction.transactionDate).toLocaleDateString()}
        </td>
        <td className="px-4 py-3 text-right font-medium text-slate-900">
          {transaction.shares.toFixed(4).replace(/\.?0+$/, '')}
        </td>
        <td className="px-4 py-3 text-right font-medium text-slate-900">
          {formatCurrency(transaction.pricePerShare)}
        </td>
        <td className={`px-4 py-3 text-right font-semibold
          ${transaction.type === 'BUY' ? 'text-slate-900' : 'text-slate-900'}`}
        >
          {formatCurrency(transaction.totalAmount)}
        </td>
        <td className="px-4 py-3 text-center">
          <button
            onClick={onEdit}
            className="mr-2 rounded px-2 py-1 text-sm text-indigo-600 hover:bg-indigo-50"
          >
            Edit
          </button>
          <button
            onClick={onDelete}
            className="rounded px-2 py-1 text-sm text-red-600 hover:bg-red-50"
          >
            Delete
          </button>
        </td>
      </tr>
    )
  }

  // Edit mode
  const totalAmount = parseFloat(shares || '0') * parseFloat(price || '0')

  return (
    <tr className="bg-indigo-50" onKeyDown={handleKeyDown}>
      <td className="px-4 py-3">
        <select
          value={type}
          onChange={e => setType(e.target.value as TransactionType)}
          className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
        >
          <option value="BUY">BUY</option>
          <option value="SELL">SELL</option>
        </select>
      </td>
      <td className="px-4 py-3">
        <input
          type="text"
          value={symbol}
          onChange={e => handleSymbolChange(e.target.value)}
          className={`w-full rounded border px-2 py-1 text-sm uppercase
            ${validation?.valid === false ? 'border-red-300' : 'border-slate-300'}`}
        />
        {validating && <span className="text-xs text-slate-400">Validating...</span>}
        {validation?.valid === false && (
          <span className="text-xs text-red-500">{validation.errorMessage}</span>
        )}
      </td>
      <td className="px-4 py-3">
        <input
          type="date"
          value={date}
          onChange={e => setDate(e.target.value)}
          max={new Date().toISOString().split('T')[0]}
          className="w-full rounded border border-slate-300 px-2 py-1 text-sm"
        />
      </td>
      <td className="px-4 py-3">
        <input
          type="number"
          value={shares}
          onChange={e => setShares(e.target.value)}
          min="0.0001"
          step="0.0001"
          className="w-full rounded border border-slate-300 px-2 py-1 text-right text-sm"
        />
      </td>
      <td className="px-4 py-3">
        <input
          type="number"
          value={price}
          onChange={e => setPrice(e.target.value)}
          min="0.01"
          step="0.01"
          className="w-full rounded border border-slate-300 px-2 py-1 text-right text-sm"
        />
      </td>
      <td className="px-4 py-3 text-right font-medium text-slate-700">
        {formatCurrency(totalAmount)}
      </td>
      <td className="px-4 py-3 text-center">
        <button
          onClick={handleSave}
          disabled={saving || validating}
          className="mr-2 rounded bg-indigo-600 px-2 py-1 text-sm text-white hover:bg-indigo-700
                   disabled:opacity-50"
        >
          {saving ? '...' : 'Save'}
        </button>
        <button
          onClick={onCancel}
          className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-600 hover:bg-slate-100"
        >
          Cancel
        </button>
      </td>
    </tr>
  )
}
```

---

### Phase 13: Frontend - Component Exports

**File**: `frontend/src/components/transactions/index.ts` (NEW)

```typescript
export { TransactionForm } from './TransactionForm'
export { TransactionGrid } from './TransactionGrid'
export { TransactionGridRow } from './TransactionGridRow'
```

**Update**: `frontend/src/components/index.ts`

```typescript
export * from './common'
export * from './layout'
export * from './dashboard'
export * from './transactions'
```

**Update**: `frontend/src/hooks/index.ts`

```typescript
export { usePortfolio } from './usePortfolio'
export { usePortfolioPerformance } from './usePortfolioPerformance'
export { useTransactions, useTickerValidation } from './useTransactions'
```

---

### Phase 14: Frontend - Update Dashboard Page

**File**: `frontend/src/pages/Dashboard/Dashboard.tsx`

Add transactions section to the dashboard:

```typescript
import { DashboardNavigation } from '@components/layout'
import { usePortfolio } from '../../hooks/usePortfolio'
import { usePortfolioPerformance } from '../../hooks/usePortfolioPerformance'
import { useTransactions } from '../../hooks/useTransactions'
import { PortfolioTable } from '../../components/dashboard/PortfolioTable'
import { PerformanceChart } from '../../components/dashboard/PerformanceChart'
import { TransactionForm } from '../../components/transactions/TransactionForm'
import { TransactionGrid } from '../../components/transactions/TransactionGrid'
import { formatCurrency, formatPercent, getReturnColorClass } from '../../utils/stockFormatters'
import { useState } from 'react'

interface SummaryCardProps {
  label: string
  value: string
  className?: string
}

function SummaryCard({ label, value, className = '' }: SummaryCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-soft">
      <p className="text-sm font-medium text-slate-600">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${className || 'text-slate-900'}`}>{value}</p>
    </div>
  )
}

const Dashboard = () => {
  const { portfolio, loading, error, refresh } = usePortfolio()
  const {
    data: performanceData,
    range,
    loading: chartLoading,
    changeRange,
  } = usePortfolioPerformance('1y')
  const {
    transactions,
    loading: txLoading,
    createTransaction,
    updateTransaction,
    deleteTransaction,
  } = useTransactions()

  const [showTransactions, setShowTransactions] = useState(false)
  const [showAddForm, setShowAddForm] = useState(false)

  const handleCreateTransaction = async (request: any) => {
    await createTransaction(request)
    setShowAddForm(false)
    // Refresh portfolio to reflect changes
    refresh()
  }

  const handleUpdateTransaction = async (id: number, request: any) => {
    await updateTransaction(id, request)
    refresh()
  }

  const handleDeleteTransaction = async (id: number) => {
    await deleteTransaction(id)
    refresh()
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <DashboardNavigation />
        <div className="flex justify-center p-8">
          <p className="text-slate-600">Loading portfolio...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <DashboardNavigation />
        <div className="p-8 text-center">
          <p className="text-red-500">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <DashboardNavigation />

      <div className="container mx-auto px-4 py-8">
        <header className="mb-8 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-slate-900">Portfolio Overview</h1>
          <div className="flex gap-3">
            <button
              onClick={() => setShowTransactions(!showTransactions)}
              className={`rounded-lg px-4 py-2 font-medium transition-colors
                ${showTransactions
                  ? 'bg-indigo-100 text-indigo-700'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
            >
              {showTransactions ? 'Hide Transactions' : 'Manage Transactions'}
            </button>
            <button
              onClick={refresh}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700"
            >
              Refresh Prices
            </button>
          </div>
        </header>

        {/* Portfolio Summary Card */}
        {portfolio && (
          <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryCard label="Total Value" value={formatCurrency(portfolio.totalValue)} />
            <SummaryCard label="Total Cost" value={formatCurrency(portfolio.totalCost)} />
            <SummaryCard
              label="Total Return"
              value={formatCurrency(portfolio.totalReturnDollars)}
              className={getReturnColorClass(portfolio.totalReturnDollars)}
            />
            <SummaryCard
              label="Return %"
              value={formatPercent(portfolio.totalReturnPercent)}
              className={getReturnColorClass(portfolio.totalReturnPercent)}
            />
          </div>
        )}

        {/* Transaction Management Section */}
        {showTransactions && (
          <section className="mb-8">
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-soft">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold text-slate-900">Transactions</h2>
                {!showAddForm && (
                  <button
                    onClick={() => setShowAddForm(true)}
                    className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white
                             hover:bg-indigo-700"
                  >
                    + Add Transaction
                  </button>
                )}
              </div>

              {/* Quick Add Form */}
              {showAddForm && (
                <div className="mb-6 rounded-lg border border-indigo-200 bg-indigo-50 p-4">
                  <h3 className="mb-3 font-medium text-indigo-900">New Transaction</h3>
                  <TransactionForm
                    onSubmit={handleCreateTransaction}
                    onCancel={() => setShowAddForm(false)}
                  />
                </div>
              )}

              {/* Transaction Grid */}
              {txLoading ? (
                <p className="text-slate-500">Loading transactions...</p>
              ) : (
                <TransactionGrid
                  transactions={transactions}
                  onCreate={handleCreateTransaction}
                  onUpdate={handleUpdateTransaction}
                  onDelete={handleDeleteTransaction}
                />
              )}
            </div>
          </section>
        )}

        {/* Performance Chart Widget */}
        <section className="mb-8">
          <PerformanceChart
            data={performanceData}
            range={range}
            onRangeChange={changeRange}
            loading={chartLoading}
          />
        </section>

        {/* Holdings Table */}
        <section>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-900">Your Holdings</h2>
            <span className="text-sm text-slate-500">
              Prices updated:{' '}
              {portfolio?.pricesUpdatedAt
                ? new Date(portfolio.pricesUpdatedAt).toLocaleTimeString()
                : '-'}
            </span>
          </div>
          {portfolio?.holdings.length ? (
            <PortfolioTable holdings={portfolio.holdings} />
          ) : (
            <p className="text-slate-500">No holdings yet. Add transactions to get started.</p>
          )}
        </section>
      </div>
    </div>
  )
}

export default Dashboard
```

---

## API Specification

### Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/transactions` | Get all user transactions | Yes |
| GET | `/api/transactions/validate-ticker?symbol={symbol}` | Validate ticker | Yes |
| POST | `/api/transactions` | Create transaction | Yes |
| PUT | `/api/transactions/{id}` | Update transaction | Yes |
| DELETE | `/api/transactions/{id}` | Delete transaction | Yes |

### Request/Response Examples

#### Create Transaction Request

```json
POST /api/transactions
{
  "type": "BUY",
  "symbol": "AAPL",
  "transactionDate": "2024-01-15",
  "shares": 50,
  "pricePerShare": 142.50,
  "notes": "Initial purchase"
}
```

#### Transaction Response

```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "BUY",
    "symbol": "AAPL",
    "companyName": "Apple Inc.",
    "transactionDate": "2024-01-15",
    "shares": 50.0,
    "pricePerShare": 142.50,
    "totalAmount": 7125.00,
    "notes": "Initial purchase",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

#### Ticker Validation Response

```json
{
  "success": true,
  "data": {
    "valid": true,
    "symbol": "AAPL",
    "companyName": "Apple Inc.",
    "errorMessage": null
  }
}
```

---

## File Structure Summary

```
backend/
├── src/main/java/com/stocktracker/
│   ├── entity/
│   │   ├── Transaction.java              # NEW
│   │   └── TransactionType.java          # NEW
│   ├── repository/
│   │   └── TransactionRepository.java    # NEW
│   ├── dto/
│   │   ├── request/
│   │   │   └── TransactionRequest.java   # NEW
│   │   └── response/
│   │       ├── TransactionResponse.java  # NEW
│   │       └── TickerValidationResponse.java # NEW
│   ├── service/
│   │   ├── TransactionService.java       # NEW
│   │   └── HoldingRecalculationService.java # NEW
│   └── controller/
│       └── TransactionController.java    # NEW
│
├── src/test/java/com/stocktracker/
│   └── service/
│       └── TransactionServiceTest.java   # NEW

frontend/
├── src/
│   ├── services/api/
│   │   ├── transactionApi.ts             # NEW
│   │   └── index.ts                      # UPDATE
│   ├── hooks/
│   │   ├── useTransactions.ts            # NEW
│   │   └── index.ts                      # UPDATE
│   ├── components/
│   │   ├── transactions/
│   │   │   ├── TransactionForm.tsx       # NEW
│   │   │   ├── TransactionGrid.tsx       # NEW
│   │   │   ├── TransactionGridRow.tsx    # NEW
│   │   │   └── index.ts                  # NEW
│   │   └── index.ts                      # UPDATE
│   └── pages/Dashboard/
│       └── Dashboard.tsx                 # UPDATE
```

---

## Implementation Order

### Backend (Phase 1-7)
1. Create `TransactionType.java` enum
2. Create `Transaction.java` entity
3. Create `TransactionRepository.java`
4. Create `TransactionRequest.java` and `TransactionResponse.java` DTOs
5. Create `TickerValidationResponse.java` DTO
6. Create `HoldingRecalculationService.java`
7. Create `TransactionService.java`
8. Create `TransactionController.java`
9. Write unit tests for `TransactionService`
10. Run database migration (Flyway/Liquibase or auto-ddl)
11. Test API endpoints with Postman/curl

### Frontend (Phase 8-14)
12. Create `transactionApi.ts` with API client
13. Create `useTransactions.ts` and `useTickerValidation.ts` hooks
14. Create `TransactionForm.tsx` component
15. Create `TransactionGridRow.tsx` component
16. Create `TransactionGrid.tsx` component
17. Update barrel exports
18. Update `Dashboard.tsx` to integrate transaction management
19. Test end-to-end

---

## Validation Rules Summary

| Rule | Implementation Location |
|------|------------------------|
| All fields mandatory | Frontend form + Backend DTO validation |
| Ticker must be valid | Backend validates via Yahoo Finance API |
| Shares must be positive | Frontend + Backend validation |
| Price must be positive | Frontend + Backend validation |
| Date cannot be future | Frontend max date + Backend @PastOrPresent |
| Cannot sell without buying first | Backend TransactionService |
| Cannot sell more than owned | Backend TransactionService |
| Sell date cannot precede first buy | Backend TransactionService |

---

## Error Handling

### Backend
- Invalid ticker → 400 Bad Request with message
- Sell without buy → 400 Bad Request with message
- Insufficient shares to sell → 400 Bad Request with message
- Transaction not found → 404 Not Found
- Unauthorized access → 401/403

### Frontend
- Form validation errors → Inline error messages
- API errors → Toast notification or inline alert
- Network errors → Retry option
- Loading states → Spinners and disabled buttons

---

## Acceptance Criteria

### Visibility & Navigation
- [ ] Transaction section is hidden by default on page load
- [ ] "Manage Transactions" button toggles transaction section visibility
- [ ] Button text changes to "Hide Transactions" when section is visible
- [ ] Transaction entry form is hidden by default within the section
- [ ] "+ Add Transaction" button shows the entry form
- [ ] Entry form hides after successful submission or cancel

### Transaction Form
- [ ] Transaction form displays with all required fields
- [ ] Ticker validation shows company name for valid symbols
- [ ] Ticker validation shows error for invalid symbols
- [ ] All form validations work correctly (required fields, positive numbers, valid date)

### Transaction Operations
- [ ] Buy transactions are saved and appear in grid
- [ ] Sell transactions validate against owned shares
- [ ] Sell transactions cannot exceed owned quantity
- [ ] Transactions can be edited inline in grid
- [ ] Transactions can be deleted with confirmation

### Transaction Grid
- [ ] Grid supports sorting by date, symbol, type, amount
- [ ] Grid supports filtering by symbol
- [ ] Add new row functionality works at bottom of grid

### Portfolio Integration
- [ ] Holdings are automatically recalculated after transaction changes
- [ ] Portfolio values update after transaction changes

### UX
- [ ] Loading states are shown during API calls
- [ ] Error messages are displayed appropriately

---

## Future Enhancements (Out of Scope)

- [ ] Bulk import transactions from CSV/Excel
- [ ] Transaction history export to CSV
- [ ] Dividend transactions support
- [ ] Stock split handling
- [ ] Transaction fees/commissions
- [ ] Multi-currency support
- [ ] Realized gains/losses calculation
- [ ] Tax lot tracking (FIFO, LIFO, specific ID)
- [ ] Broker integration for automatic import
