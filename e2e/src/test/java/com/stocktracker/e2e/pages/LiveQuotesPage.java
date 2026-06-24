package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

/** Dashboard live-data surfaces: last-updated indicator and base-currency select. */
public class LiveQuotesPage {

  private static final By LAST_UPDATED = By.cssSelector("[data-testid='quote-last-updated']");
  private static final By SYMBOL_SEARCH = By.cssSelector("[data-testid='symbol-search']");
  private static final By BASE_CURRENCY = By.cssSelector("[data-testid='base-currency-select']");
  private static final By BASE_CURRENCY_OPTIONS =
      By.cssSelector("[data-testid='base-currency-select'] option");

  private final WebDriver driver;
  private final Waits waits;

  public LiveQuotesPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public LiveQuotesPage waitLoaded() {
    waits.untilVisible(LAST_UPDATED);
    waits.untilVisible(BASE_CURRENCY);
    return this;
  }

  public String lastUpdatedText() {
    return driver.findElement(LAST_UPDATED).getText().trim();
  }

  public boolean hasBaseCurrencySelect() {
    return !driver.findElements(BASE_CURRENCY).isEmpty();
  }

  public boolean hasSymbolSearch() {
    return !driver.findElements(SYMBOL_SEARCH).isEmpty();
  }

  public LiveQuotesPage selectBaseCurrency(String currency) {
    var select = waits.untilVisible(BASE_CURRENCY);
    waits.untilTrue(
        d ->
            d.findElements(BASE_CURRENCY_OPTIONS).stream()
                .anyMatch(option -> option.getText().trim().equals(currency)));
    new Select(select).selectByVisibleText(currency);
    return this;
  }
}
