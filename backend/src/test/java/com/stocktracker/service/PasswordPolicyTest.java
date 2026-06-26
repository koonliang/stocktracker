package com.stocktracker.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
  @Test
  void acceptsTrimmedEmailWithDomain() {
    assertTrue(PasswordPolicy.isValidEmail("  user@example.com  "));
  }

  @Test
  void rejectsMissingOrMalformedEmail() {
    assertFalse(PasswordPolicy.isValidEmail(null));
    assertFalse(PasswordPolicy.isValidEmail("user@"));
  }

  @Test
  void reportsAllMissingPasswordRequirements() {
    assertIterableEquals(
        List.of(
            "Password must be at least 8 characters",
            "Password must contain a lowercase letter",
            "Password must contain a digit"),
        PasswordPolicy.passwordViolations("SHORT"));
  }

  @Test
  void returnsNoViolationsForStrongPassword() {
    assertTrue(PasswordPolicy.passwordViolations("Passw0rd!").isEmpty());
  }
}
