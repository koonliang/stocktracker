package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.pages.DashboardPage;
import com.stocktracker.e2e.pages.ForgotPasswordPage;
import com.stocktracker.e2e.pages.LoginPage;
import com.stocktracker.e2e.pages.SignupPage;
import com.stocktracker.e2e.support.BaseTest;
import com.stocktracker.e2e.support.DevTokenClient;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Authentication regression journey (contracts/e2e-journey.md, FR-T03/T05). Runs headlessly against
 * docker-compose in {@code STOCKTRACKER_AUTH_MODE=dev}: sign-up→sign-in→sign-out, invalid
 * credentials, protected-route redirect, password reset, and per-user data isolation.
 */
class AuthJourneyTest extends BaseTest {

  private static final String DEV_PASSWORD = "DevPass123!";
  private static final String SEED_USER = "seed@stocktracker.local";
  private static final String EMPTY_USER = "empty@stocktracker.local";
  private static final AtomicInteger COUNTER = new AtomicInteger();

  private static final By APP_SHELL = By.cssSelector("[data-testid='app-shell-authenticated']");
  private static final By LOGOUT = By.cssSelector("[data-testid='logout-button']");
  private static final By LOGIN_EMAIL = By.cssSelector("[data-testid='login-email']");
  private static final By RESET_PASSWORD = By.cssSelector("[data-testid='reset-password']");
  private static final By RESET_SUBMIT = By.cssSelector("[data-testid='reset-submit']");
  private static final By RESET_CONFIRMATION = By.cssSelector("[data-testid='reset-confirmation']");
  private static final By EMPTY_PORTFOLIO =
      By.xpath("//*[contains(text(),'Your portfolio is empty.')]");

  private final DevTokenClient devTokens = new DevTokenClient();

  @Test
  void signUpSignInSignOut() {
    var email = uniqueEmail("journey");

    new SignupPage(driver, waits, baseUrl()).open().signUp(email, "Passw0rd!");

    new LoginPage(driver, waits, baseUrl()).open().signIn(email, "Passw0rd!");
    waits.untilVisible(APP_SHELL);

    signOut();
    waits.untilVisible(LOGIN_EMAIL);
    assertThat(driver.getCurrentUrl()).contains("/login");
  }

  @Test
  void invalidCredentialsAreRejected() {
    var login = new LoginPage(driver, waits, baseUrl()).open();
    login.signIn(SEED_USER, "WrongPass1!");

    assertThat(login.errorText()).isNotBlank();
    assertThat(driver.findElements(APP_SHELL)).isEmpty();
    assertThat(driver.getCurrentUrl()).contains("/login");
  }

  @Test
  void protectedRouteRedirectsAnonymousUser() {
    driver.get(baseUrl() + "/transactions");

    waits.untilVisible(LOGIN_EMAIL);
    assertThat(driver.getCurrentUrl()).contains("/login");
  }

  @Test
  void passwordResetReplacesCredential() {
    var email = uniqueEmail("reset");

    // A self-contained account so the reset never mutates a shared seed user.
    new SignupPage(driver, waits, baseUrl()).open().signUp(email, "Passw0rd!");

    new ForgotPasswordPage(driver, waits, baseUrl()).open().requestReset(email);
    var token = devTokens.latestResetToken(email);

    driver.get(baseUrl() + "/reset-password?token=" + token);
    var passwordField = waits.untilVisible(RESET_PASSWORD);
    passwordField.clear();
    passwordField.sendKeys("NewPass456!");
    waits.untilClickable(RESET_SUBMIT).click();
    waits.untilVisible(RESET_CONFIRMATION);

    // New password works...
    new LoginPage(driver, waits, baseUrl()).open().signIn(email, "NewPass456!");
    waits.untilVisible(APP_SHELL);
    signOut();

    // ...and the old password is now rejected.
    var login = new LoginPage(driver, waits, baseUrl()).open();
    login.signIn(email, "Passw0rd!");
    assertThat(login.errorText()).isNotBlank();
    assertThat(driver.findElements(APP_SHELL)).isEmpty();
  }

  @Test
  void perUserDataIsolation() {
    // User A (seed) owns demo data — the holdings table renders with rows.
    new LoginPage(driver, waits, baseUrl()).open().signIn(SEED_USER, DEV_PASSWORD);
    waits.untilVisible(APP_SHELL);
    var dashboard = new DashboardPage(driver, waits).waitLoaded();
    assertThat(dashboard.holdingsRowCount()).isGreaterThan(0);

    signOut();
    waits.untilVisible(LOGIN_EMAIL);

    // User B owns nothing — never sees A's positions; the empty-portfolio state is shown instead.
    new LoginPage(driver, waits, baseUrl()).open().signIn(EMPTY_USER, DEV_PASSWORD);
    waits.untilVisible(APP_SHELL);
    waits.untilVisible(EMPTY_PORTFOLIO);
    assertThat(driver.findElements(By.cssSelector("[data-testid='holdings-table']"))).isEmpty();
  }

  @Test
  void demoUsersCanBeCreatedReusedAndEventuallyHitTheThreeUserCap() {
    var login = new LoginPage(driver, waits, baseUrl()).open();
    assertThat(login.hasNonProdBanner()).isTrue();
    assertThat(login.hasSocialActions()).isTrue();

    while (login.demoUserCount() < 3) {
      login.createDemoUser();
      waits.untilVisible(APP_SHELL);
      new DashboardPage(driver, waits).waitLoaded();
      signOut();
      login = new LoginPage(driver, waits, baseUrl()).open();
    }

    login.loginAsDemoUser(1);
    waits.untilVisible(APP_SHELL);
    new DashboardPage(driver, waits).waitLoaded();

    signOut();
    login = new LoginPage(driver, waits, baseUrl()).open();

    assertThat(login.isCreateDemoUserDisabled()).isTrue();
    assertThat(login.createDemoUserText()).contains("All Demo Slots In Use");
    assertThat(login.demoUserCount()).isEqualTo(3);
  }

  @Test
  void failedSocialCallbackShowsErrorWithoutCreatingASession() {
    var state = java.util.Base64.getEncoder().encodeToString("{\"from\":\"/\",\"provider\":\"google\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    driver.get(baseUrl() + "/auth/callback?code=invalid-social-code&state=" + state);

    var login = new LoginPage(driver, waits, baseUrl());
    assertThat(login.callbackErrorText()).contains("couldn't complete your sign-in");
    assertThat(driver.findElements(APP_SHELL)).isEmpty();
  }

  private void signOut() {
    waits.untilClickable(LOGOUT).click();
    waits.untilVisible(LOGIN_EMAIL);
  }

  private static String uniqueEmail(String prefix) {
    return "e2e-%s-%d-%d@example.com"
        .formatted(prefix, System.currentTimeMillis(), COUNTER.incrementAndGet());
  }
}
