package com.stocktracker.persistence;

import com.stocktracker.domain.VerificationToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class VerificationTokenRepository implements PanacheRepositoryBase<VerificationToken, Long> {
  public Optional<VerificationToken> findByHash(String tokenHash) {
    return find("tokenHash", tokenHash).firstResultOptional();
  }

  /** Latest still-usable token for a user+purpose (used by the dev token endpoint). */
  public Optional<VerificationToken> findLatestUsable(
      Long userId, VerificationToken.Purpose purpose, LocalDateTime now) {
    return find(
            "userId = ?1 and purpose = ?2 and consumedAt is null and expiresAt > ?3"
                + " order by createdAt desc",
            userId,
            purpose,
            now)
        .firstResultOptional();
  }

  /** Marks prior unconsumed tokens of this purpose consumed so only the newest is valid. */
  public void supersedePrior(Long userId, VerificationToken.Purpose purpose, LocalDateTime now) {
    update(
        "consumedAt = ?1 where userId = ?2 and purpose = ?3 and consumedAt is null",
        now,
        userId,
        purpose);
  }

  public long deleteExpiredOrConsumed(LocalDateTime now) {
    return delete("consumedAt is not null or expiresAt < ?1", now);
  }
}
