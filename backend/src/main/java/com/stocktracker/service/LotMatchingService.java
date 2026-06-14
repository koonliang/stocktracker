package com.stocktracker.service;

import com.stocktracker.domain.PortfolioTransaction;
import com.stocktracker.service.CostBasisEngine.MatchingMethod;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class LotMatchingService {
  @Inject CostBasisEngine costBasisEngine;

  public CostBasisEngine.Result match(List<PortfolioTransaction> transactions, String method) {
    return costBasisEngine.replay(transactions, MatchingMethod.parse(method));
  }
}
