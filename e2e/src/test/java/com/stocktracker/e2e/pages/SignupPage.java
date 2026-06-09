package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Public sign-up route {@code /signup}, driven via its {@code data-testid} hooks. */
public class SignupPage {

  private static final By EMAIL = By.cssSelector("[data-testid='signup-email']");
  private static final By PASSWORD = By.cssSelector("[data-testid='signup-password']");
  private static final By SUBMIT = By.cssSelector("[data-testid='signup-submit']");
  private static final By CONFIRMATION = By.cssSelector("[data-testid='signup-success']");

  private final WebDriver driver;
  private final Waits waits;
  private final String baseUrl;

  public SignupPage(WebDriver driver, Waits waits, String baseUrl) {
    this.driver = driver;
    this.waits = waits;
    this.baseUrl = baseUrl;
  }

  public SignupPage open() {
    driver.get(baseUrl + "/signup");
    waits.untilVisible(EMAIL);
    return this;
  }

  /**
   * Submits the sign-up form and waits for the "check your email" confirmation. The confirmation
   * only renders after the {@code /api/auth/signup} POST resolves, so callers can safely fetch the
   * verification token afterwards (the backend has recorded it by then).
   */
  public void signUp(String email, String password) {
    var emailField = waits.untilVisible(EMAIL);
    emailField.clear();
    emailField.sendKeys(email);
    var passwordField = driver.findElement(PASSWORD);
    passwordField.clear();
    passwordField.sendKeys(password);
    waits.untilClickable(SUBMIT).click();
    waits.untilVisible(CONFIRMATION);
  }
}
