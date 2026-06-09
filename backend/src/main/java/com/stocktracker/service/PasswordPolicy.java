package com.stocktracker.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Documented password-strength and email-format policy (FR-010/FR-017). Enforced identically at
 * sign-up and password reset. Returns the list of unmet rules so the API can surface them.
 */
public final class PasswordPolicy {
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private PasswordPolicy() {}

  public static boolean isValidEmail(String email) {
    return email != null && EMAIL.matcher(email.trim()).matches();
  }

  /** Empty when the password satisfies every rule; otherwise the human-readable unmet rules. */
  public static List<String> passwordViolations(String password) {
    var violations = new ArrayList<String>();
    if (password == null || password.length() < 8) {
      violations.add("Password must be at least 8 characters");
    }
    if (password == null || password.chars().noneMatch(Character::isUpperCase)) {
      violations.add("Password must contain an uppercase letter");
    }
    if (password == null || password.chars().noneMatch(Character::isLowerCase)) {
      violations.add("Password must contain a lowercase letter");
    }
    if (password == null || password.chars().noneMatch(Character::isDigit)) {
      violations.add("Password must contain a digit");
    }
    return violations;
  }
}
