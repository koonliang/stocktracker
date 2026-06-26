package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocktracker.domain.VerificationToken.Purpose;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DevTokenStoreTest {
  @Test
  void returnsLatestNonExpiredEntryUsingNormalizedKey() {
    var store = new DevTokenStore();
    store.record(
        "  User@Example.com ", Purpose.PASSWORD_RESET, "raw-token", LocalDateTime.now().plusMinutes(5));

    var entry = store.latest("user@example.com", Purpose.PASSWORD_RESET);

    assertTrue(entry.isPresent());
    assertEquals("raw-token", entry.get().token());
  }

  @Test
  void ignoresExpiredEntries() {
    var store = new DevTokenStore();
    store.record(
        "user@example.com", Purpose.EMAIL_VERIFICATION, "expired", LocalDateTime.now().minusSeconds(1));

    assertTrue(store.latest("user@example.com", Purpose.EMAIL_VERIFICATION).isEmpty());
  }
}
