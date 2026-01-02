package com.stocktracker.repository;

import com.stocktracker.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    /**
     * Find all holdings for a specific user, ordered by symbol ascending.
     */
    List<Holding> findByUserIdOrderBySymbolAsc(Long userId);

    /**
     * Find a specific holding for a user by symbol.
     */
    Optional<Holding> findByUserIdAndSymbol(Long userId, String symbol);

    /**
     * Check if a user already has a holding for a specific symbol.
     */
    boolean existsByUserIdAndSymbol(Long userId, String symbol);

    /**
     * Delete all holdings for a user (used for demo account cleanup).
     */
    void deleteByUserId(Long userId);
}
