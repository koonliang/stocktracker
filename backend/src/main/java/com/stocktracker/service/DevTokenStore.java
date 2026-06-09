package com.stocktracker.service;

import com.stocktracker.domain.VerificationToken.Purpose;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the most recent raw verification/reset token per email+purpose so the dev-only token
 * endpoint can return it (FR-T02). The database only ever stores the SHA-256 hash; this in-memory
 * mirror exists solely to let the e2e suite drive verify/reset without an inbox. Read only by
 * {@code DevAuthTokenResource}, which is itself gated to dev mode.
 */
@ApplicationScoped
public class DevTokenStore {
  public record Entry(String token, Purpose purpose, LocalDateTime expiresAt) {}

  private final Map<String, Entry> latest = new ConcurrentHashMap<>();

  public void record(String email, Purpose purpose, String rawToken, LocalDateTime expiresAt) {
    latest.put(key(email, purpose), new Entry(rawToken, purpose, expiresAt));
  }

  public Optional<Entry> latest(String email, Purpose purpose) {
    return Optional.ofNullable(latest.get(key(email, purpose)))
        .filter(entry -> entry.expiresAt().isAfter(LocalDateTime.now()));
  }

  private String key(String email, Purpose purpose) {
    return email.trim().toLowerCase(Locale.ROOT) + "|" + purpose;
  }
}
