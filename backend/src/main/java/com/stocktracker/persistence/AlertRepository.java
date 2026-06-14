package com.stocktracker.persistence;

import com.stocktracker.domain.Alert;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AlertRepository implements PanacheRepositoryBase<Alert, Long> {
  public List<Alert> listForUser(Long userId) {
    return list("userId = ?1 order by createdAt desc", userId);
  }

  public List<Alert> listForSymbol(String symbol) {
    return list("upper(instrumentSymbol) = ?1 order by id", symbol.toUpperCase());
  }

  public Optional<Alert> findByIdAndUser(Long id, Long userId) {
    return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
  }
}
