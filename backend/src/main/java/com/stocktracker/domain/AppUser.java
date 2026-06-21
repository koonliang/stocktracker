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
import java.util.Locale;

@Entity
@Table(name = "app_user")
public class AppUser extends PanacheEntityBase {
  public enum AccountKind {
    STANDARD,
    DEMO
  }

  public enum Status {
    UNVERIFIED,
    ACTIVE,
    LOCKED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 320)
  public String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  public Status status = Status.UNVERIFIED;

  @Column(name = "email_verified", nullable = false)
  public boolean emailVerified = false;

  /** User-chosen reporting currency for combined totals/P&L (FR-031). */
  @Column(name = "base_currency", nullable = false, length = 3)
  public String baseCurrency = "USD";

  @Enumerated(EnumType.STRING)
  @Column(name = "account_kind", nullable = false, length = 16)
  public AccountKind accountKind = AccountKind.STANDARD;

  @Column(name = "display_name", length = 120)
  public String displayName;

  @Column(name = "demo_slot")
  public Byte demoSlot;

  @Column(name = "demo_last_activated_at")
  public LocalDateTime demoLastActivatedAt;

  @Column(name = "demo_seed_profile", length = 32)
  public String demoSeedProfile;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @Column(name = "last_login_at")
  public LocalDateTime lastLoginAt;

  @Column(name = "sessions_invalid_before")
  public LocalDateTime sessionsInvalidBefore;

  @Column(name = "sessions_invalid_before_ms")
  public Long sessionsInvalidBeforeMs;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  /** Trims and lowercases an email so each account maps to one normalized identity (FR-014). */
  public static String normalizeEmail(String raw) {
    return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
  }
}
