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
  private static final By NONPROD_BANNER = By.cssSelector("[data-testid='nonprod-auth-banner']");
  private static final By SOCIAL_GOOGLE = By.cssSelector("[data-testid='social-login-google']");
  private static final By SOCIAL_FACEBOOK = By.cssSelector("[data-testid='social-login-facebook']");
  private static final By DEMO_CREATE = By.cssSelector("[data-testid='demo-user-create']");
  private static final By DEMO_LIST = By.cssSelector("[data-testid='demo-user-list']");
  private static final By DEMO_LIMIT_MESSAGE =
      By.cssSelector("[data-testid='demo-user-limit-message']");
  private static final By CALLBACK_ERROR =
      By.xpath("//*[contains(text(),'We couldn') and contains(text(),'complete your sign-in')]");

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

  public boolean hasNonProdBanner() {
    return waits.untilVisible(NONPROD_BANNER).isDisplayed();
  }

  public boolean hasSocialActions() {
    return waits.untilVisible(SOCIAL_GOOGLE).isDisplayed()
        && waits.untilVisible(SOCIAL_FACEBOOK).isDisplayed();
  }

  public int demoUserCount() {
    waits.untilPresent(DEMO_LIST);
    return driver.findElements(By.cssSelector("[data-testid^='demo-user-login-']")).size();
  }

  public void createDemoUser() {
    waits.untilClickable(DEMO_CREATE).click();
  }

  public void loginAsDemoUser(int slot) {
    waits.untilClickable(By.cssSelector("[data-testid='demo-user-login-" + slot + "']")).click();
  }

  public String demoLimitMessageText() {
    return waits.untilVisible(DEMO_LIMIT_MESSAGE).getText().trim();
  }

  public boolean isCreateDemoUserDisabled() {
    return waits.untilPresent(DEMO_CREATE).getDomAttribute("disabled") != null;
  }

  public String createDemoUserText() {
    return waits.untilPresent(DEMO_CREATE).getText().trim();
  }

  public String callbackErrorText() {
    return waits.untilVisible(CALLBACK_ERROR).getText().trim();
  }
}
