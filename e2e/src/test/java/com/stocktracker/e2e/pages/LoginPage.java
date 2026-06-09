package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Public sign-in route {@code /login}, driven via its {@code data-testid} hooks. */
public class LoginPage {

  private static final By EMAIL = By.cssSelector("[data-testid='login-email']");
  private static final By PASSWORD = By.cssSelector("[data-testid='login-password']");
  private static final By SUBMIT = By.cssSelector("[data-testid='login-submit']");
  private static final By ERROR = By.cssSelector("[data-testid='login-error']");

  private final WebDriver driver;
  private final Waits waits;
  private final String baseUrl;

  public LoginPage(WebDriver driver, Waits waits, String baseUrl) {
    this.driver = driver;
    this.waits = waits;
    this.baseUrl = baseUrl;
  }

  public LoginPage open() {
    driver.get(baseUrl + "/login");
    waits.untilVisible(EMAIL);
    return this;
  }

  public void signIn(String email, String password) {
    var emailField = waits.untilVisible(EMAIL);
    emailField.clear();
    emailField.sendKeys(email);
    var passwordField = driver.findElement(PASSWORD);
    passwordField.clear();
    passwordField.sendKeys(password);
    waits.untilClickable(SUBMIT).click();
  }

  public String errorText() {
    return waits.untilVisible(ERROR).getText().trim();
  }
}
