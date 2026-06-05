package com.stocktracker.e2e.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Base class for every journey. Owns the WebDriver lifecycle (a fresh browser per test, quit
 * afterwards) and resolves the frontend base URL from {@code -De2e.baseUrl} (default {@code
 * http://localhost:5173}).
 */
public abstract class BaseTest {

  /** The application shell renders {@code <main id="main">}; we treat it as the landing element. */
  private static final By APP_SHELL = By.id("main");

  private static final String DEFAULT_BASE_URL = "http://localhost:5173";

  protected WebDriver driver;
  protected Waits waits;

  @BeforeEach
  void startBrowser() {
    driver = DriverFactory.create();
    waits = new Waits(driver);
  }

  @AfterEach
  void quitBrowser() {
    if (driver != null) {
      driver.quit();
    }
  }

  protected String baseUrl() {
    return System.getProperty("e2e.baseUrl", DEFAULT_BASE_URL);
  }

  /**
   * Navigates to {@code path} (relative to the base URL) and waits for the app shell to render
   * before returning, so journeys never assert against a half-loaded page (FR-010).
   */
  protected void open(String path) {
    String url = baseUrl() + path;
    driver.get(url);
    waits.untilVisible(APP_SHELL);
  }
}
