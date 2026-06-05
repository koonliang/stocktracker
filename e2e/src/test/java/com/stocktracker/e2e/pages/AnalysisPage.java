package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Instrument analysis at route {@code /analysis/:ticker}: price chart + key statistics. */
public class AnalysisPage {

  private static final By PRICE_CHART = By.cssSelector("[data-testid='price-chart']");
  private static final By KEY_STATS_GRID = By.cssSelector("[data-testid='key-stats-grid']");

  private final WebDriver driver;
  private final Waits waits;

  public AnalysisPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public AnalysisPage waitLoaded() {
    waits.untilVisible(PRICE_CHART);
    waits.untilVisible(KEY_STATS_GRID);
    return this;
  }

  public boolean isPriceChartVisible() {
    return driver.findElement(PRICE_CHART).isDisplayed();
  }

  public String keyStatsText() {
    return driver.findElement(KEY_STATS_GRID).getText().trim();
  }
}
