package com.stocktracker.e2e.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.stocktracker.e2e.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Minimal gateable check that the full stack is reachable and the app shell renders. If this fails
 * in CI the environment never came up; journey failures are a separate, more specific signal.
 */
class SmokeTest extends BaseTest {

  @Test
  void appShellLoads() {
    signInAsSeedUser();
    open("/");

    assertThat(driver.getTitle()).isEqualTo("StockTracker");
    assertThat(driver.findElement(By.id("main")).isDisplayed()).isTrue();
  }
}
