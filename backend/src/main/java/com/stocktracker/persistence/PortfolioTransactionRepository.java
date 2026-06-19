package com.stocktracker.persistence;

import com.stocktracker.domain.PortfolioTransaction;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PortfolioTransactionRepository
    implements PanacheRepositoryBase<PortfolioTransaction, Long> {
  public List<PortfolioTransaction> listAscending(Long userId) {
    return list("userId = ?1 order by tradeDate asc, createdAt asc", userId);
  }

  public List<PortfolioTransaction> listDescending(Long userId) {
    return list("userId = ?1 order by tradeDate desc, createdAt desc", userId);
  }

  public Optional<PortfolioTransaction> findByIdAndUser(Long id, Long userId) {
    return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
  }

  // Unscoped ordering helpers retained for repository-level tests.
  public List<PortfolioTransaction> listAscending() {
    return list("order by tradeDate asc, createdAt asc");
  }

  public List<PortfolioTransaction> listDescending() {
    return list("order by tradeDate desc, createdAt desc");
  }

  public List<PortfolioTransaction> findMissingCurrency(Long userId) {
    return list("userId = ?1 and currency is null order by tradeDate asc", userId);
  }

  public long countMissingCurrency(Long userId) {
    return count("userId = ?1 and currency is null", userId);
  }
}
