package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AlertsPage {
  private static final By PAGE = By.cssSelector("[data-testid='alerts-page']");
  private static final By SYMBOL = By.cssSelector("[data-testid='alert-symbol']");
  private static final By THRESHOLD = By.cssSelector("[data-testid='alert-threshold']");
  private static final By SUBMIT = By.cssSelector("[data-testid='alert-submit']");
  private static final By ROW = By.cssSelector("[data-testid='alert-row']");

  private final WebDriver driver;
  private final Waits waits;

  public AlertsPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public AlertsPage waitLoaded() {
    waits.untilVisible(PAGE);
    return this;
  }

  public AlertsPage createPriceAlert(String symbol, String threshold) {
    var symbolInput = driver.findElement(SYMBOL);
    symbolInput.clear();
    symbolInput.sendKeys(symbol);
    var thresholdInput = driver.findElement(THRESHOLD);
    thresholdInput.clear();
    thresholdInput.sendKeys(threshold);
    driver.findElement(SUBMIT).click();
    waits.untilVisible(ROW);
    return this;
  }
}
