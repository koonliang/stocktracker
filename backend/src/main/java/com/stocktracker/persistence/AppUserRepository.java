package com.stocktracker.persistence;

import com.stocktracker.domain.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AppUserRepository implements PanacheRepositoryBase<AppUser, Long> {
  public Optional<AppUser> findByNormalizedEmail(String rawEmail) {
    return find("email", AppUser.normalizeEmail(rawEmail)).firstResultOptional();
  }

  public boolean existsByNormalizedEmail(String rawEmail) {
    return count("email", AppUser.normalizeEmail(rawEmail)) > 0;
  }

  public Optional<AppUser> findDemoUserBySlot(int slot) {
    return find("accountKind = ?1 and demoSlot = ?2", AppUser.AccountKind.DEMO, slot)
        .firstResultOptional();
  }

  public List<AppUser> listDemoUsers() {
    return list("accountKind", AppUser.AccountKind.DEMO);
  }
}
