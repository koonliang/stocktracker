package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Portfolio dashboard at route {@code /}: holdings table + summary tiles. */
public class DashboardPage {

  private static final By HOLDINGS_TABLE = By.cssSelector("[data-testid='holdings-table']");
  private static final By SUMMARY_TILES = By.cssSelector("[data-testid='summary-tiles']");

  private final WebDriver driver;
  private final Waits waits;

  public DashboardPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  /** Waits until the seeded dashboard has rendered its holdings and summary. */
  public DashboardPage waitLoaded() {
    waits.untilVisible(SUMMARY_TILES);
    waits.untilVisible(HOLDINGS_TABLE);
    return this;
  }

  public int holdingsRowCount() {
    return driver.findElements(By.cssSelector("[data-testid='holdings-table'] tbody tr")).size();
  }

  public String summaryTilesText() {
    return driver.findElement(SUMMARY_TILES).getText().trim();
  }
}
