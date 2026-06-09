package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Public forgot-password route {@code /forgot-password}, driven via its {@code data-testid} hooks.
 */
public class ForgotPasswordPage {

  private static final By EMAIL = By.cssSelector("[data-testid='forgot-email']");
  private static final By SUBMIT = By.cssSelector("[data-testid='forgot-submit']");
  private static final By CONFIRMATION = By.cssSelector("[data-testid='forgot-confirmation']");

  private final WebDriver driver;
  private final Waits waits;
  private final String baseUrl;

  public ForgotPasswordPage(WebDriver driver, Waits waits, String baseUrl) {
    this.driver = driver;
    this.waits = waits;
    this.baseUrl = baseUrl;
  }

  public ForgotPasswordPage open() {
    driver.get(baseUrl + "/forgot-password");
    waits.untilVisible(EMAIL);
    return this;
  }

  /**
   * Submits the forgot-password form and waits for the neutral confirmation (shown regardless of
   * whether the email exists). The confirmation only renders after the {@code
   * /api/auth/forgot-password} POST resolves, so callers can safely fetch the reset token
   * afterwards (the backend has recorded it by then).
   */
  public void requestReset(String email) {
    var emailField = waits.untilVisible(EMAIL);
    emailField.clear();
    emailField.sendKeys(email);
    waits.untilClickable(SUBMIT).click();
    waits.untilVisible(CONFIRMATION);
  }
}
