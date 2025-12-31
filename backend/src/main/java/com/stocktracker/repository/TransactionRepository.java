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
