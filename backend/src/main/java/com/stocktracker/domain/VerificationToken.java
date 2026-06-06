package com.stocktracker.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Single-use, time-limited challenge for email verification or password reset. */
@Entity
@Table(name = "verification_token")
public class VerificationToken extends PanacheEntityBase {
  public enum Purpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  public Purpose purpose;

  // SHA-256 hex of the opaque token; the raw token is only ever emailed.
  @Column(name = "token_hash", nullable = false, length = 64)
  public String tokenHash;

  @Column(name = "expires_at", nullable = false)
  public LocalDateTime expiresAt;

  @Column(name = "consumed_at")
  public LocalDateTime consumedAt;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public boolean isUsable(LocalDateTime now) {
    return consumedAt == null && expiresAt.isAfter(now);
  }
}
