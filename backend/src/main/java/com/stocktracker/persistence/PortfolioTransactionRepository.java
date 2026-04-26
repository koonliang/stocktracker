package com.stocktracker.persistence;

import com.stocktracker.domain.PortfolioTransaction;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PortfolioTransactionRepository
    implements PanacheRepositoryBase<PortfolioTransaction, Long> {
  public List<PortfolioTransaction> listAscending() {
    return list("order by tradeDate asc, createdAt asc");
  }

  public List<PortfolioTransaction> listDescending() {
    return list("order by tradeDate desc, createdAt desc");
  }
}
