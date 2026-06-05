package com.stocktracker.e2e.support;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Explicit-wait helpers. The suite never uses {@code Thread.sleep} for synchronisation (FR-007);
 * every wait polls for a concrete condition so runs stay deterministic and fast.
 *
 * <p>An optional debug-only slow-motion delay (`-De2e.slowMo=<ms>`, default 0) pauses after each
 * wait so a headed run is watchable. It is opt-in and off by default, so it never affects headless
 * or CI runs.
 */
public final class Waits {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

  /** Debug-only pause between interactions; 0 (the default) means no delay. */
  private static final long SLOW_MO_MS = Long.getLong("e2e.slowMo", 0L);

  private final WebDriverWait wait;

  public Waits(WebDriver driver) {
    this(driver, DEFAULT_TIMEOUT);
  }

  public Waits(WebDriver driver, Duration timeout) {
    this.wait = new WebDriverWait(driver, timeout);
  }

  public WebElement untilVisible(By locator) {
    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    slowMo();
    return element;
  }

  public WebElement untilPresent(By locator) {
    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    slowMo();
    return element;
  }

  public WebElement untilClickable(By locator) {
    WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
    slowMo();
    return element;
  }

  public boolean untilInvisible(By locator) {
    boolean gone = wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    slowMo();
    return gone;
  }

  public boolean untilTrue(java.util.function.Function<WebDriver, Boolean> condition) {
    return wait.until(condition);
  }

  /**
   * Debug aid only: pauses so a headed run is observable. No-op unless {@code e2e.slowMo} is set.
   */
  private static void slowMo() {
    if (SLOW_MO_MS <= 0) {
      return;
    }
    try {
      Thread.sleep(SLOW_MO_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
