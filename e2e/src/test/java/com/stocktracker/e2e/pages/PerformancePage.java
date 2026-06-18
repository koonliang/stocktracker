package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class PerformancePage {
  private static final By PAGE = By.cssSelector("[data-testid='performance-page']");
  private static final By CHART = By.cssSelector("[data-testid='return-chart']");
  private static final By REALIZED = By.cssSelector("[data-testid='realized-table']");
  private static final By CONTRIBUTIONS = By.cssSelector("[data-testid='contribution-table']");
  private static final By METHOD = By.cssSelector("[data-testid='lot-method-toggle']");
  private static final By WINDOW = By.cssSelector("[data-testid='perf-window-select']");

  private final WebDriver driver;
  private final Waits waits;

  public PerformancePage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public PerformancePage waitLoaded() {
    waits.untilVisible(PAGE);
    waits.untilVisible(CHART);
    waits.untilVisible(REALIZED);
    waits.untilVisible(CONTRIBUTIONS);
    return this;
  }

  public void chooseLifo() {
    driver.findElement(METHOD).findElement(By.xpath(".//button[contains(., 'lifo')]")).click();
  }

  public void chooseAllWindow() {
    driver.findElement(WINDOW).findElement(By.xpath(".//button[contains(., 'ALL')]")).click();
  }

  public String pageText() {
    return driver.findElement(PAGE).getText().trim();
  }
}
