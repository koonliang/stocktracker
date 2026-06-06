package com.stocktracker.persistence;

import com.stocktracker.domain.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class AppUserRepository implements PanacheRepositoryBase<AppUser, Long> {
  public Optional<AppUser> findByNormalizedEmail(String rawEmail) {
    return find("email", AppUser.normalizeEmail(rawEmail)).firstResultOptional();
  }

  public boolean existsByNormalizedEmail(String rawEmail) {
    return count("email", AppUser.normalizeEmail(rawEmail)) > 0;
  }
}
