package com.stocktracker.e2e.support;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Explicit-wait helpers. The suite never uses {@code Thread.sleep} (FR-007); every wait polls for a
 * concrete condition so runs stay deterministic and fast.
 */
public final class Waits {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

  private final WebDriverWait wait;

  public Waits(WebDriver driver) {
    this(driver, DEFAULT_TIMEOUT);
  }

  public Waits(WebDriver driver, Duration timeout) {
    this.wait = new WebDriverWait(driver, timeout);
  }

  public WebElement untilVisible(By locator) {
    return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
  }

  public WebElement untilPresent(By locator) {
    return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
  }

  public WebElement untilClickable(By locator) {
    return wait.until(ExpectedConditions.elementToBeClickable(locator));
  }
}
