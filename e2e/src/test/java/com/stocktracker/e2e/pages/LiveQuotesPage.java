package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

/** Dashboard live-data surfaces: last-updated indicator, symbol search/add, base-currency select. */
public class LiveQuotesPage {

  private static final By LAST_UPDATED = By.cssSelector("[data-testid='quote-last-updated']");
  private static final By SYMBOL_SEARCH = By.cssSelector("[data-testid='symbol-search']");
  private static final By SYMBOL_RESULT = By.cssSelector("[data-testid='symbol-search-result']");
  private static final By SYMBOL_ADD = By.cssSelector("[data-testid='symbol-add']");
  private static final By BASE_CURRENCY = By.cssSelector("[data-testid='base-currency-select']");

  private final WebDriver driver;
  private final Waits waits;

  public LiveQuotesPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  public LiveQuotesPage waitLoaded() {
    waits.untilVisible(LAST_UPDATED);
    waits.untilVisible(SYMBOL_SEARCH);
    return this;
  }

  public String lastUpdatedText() {
    return driver.findElement(LAST_UPDATED).getText().trim();
  }

  public boolean hasBaseCurrencySelect() {
    return !driver.findElements(BASE_CURRENCY).isEmpty();
  }

  public LiveQuotesPage selectBaseCurrency(String currency) {
    new Select(waits.untilVisible(BASE_CURRENCY)).selectByVisibleText(currency);
    return this;
  }

  /** Type a query and wait until a result row containing {@code expectedSymbol} appears. */
  public LiveQuotesPage search(String query, String expectedSymbol) {
    waits.untilVisible(SYMBOL_SEARCH).sendKeys(query);
    waits.untilTrue(
        d ->
            d.findElements(SYMBOL_RESULT).stream()
                .anyMatch(row -> row.getText().contains(expectedSymbol)));
    return this;
  }

  /** Click the Add button on the first search result and wait until the results clear. */
  public LiveQuotesPage addFirstResult() {
    waits.untilClickable(SYMBOL_ADD).click();
    waits.untilTrue(d -> d.findElements(SYMBOL_RESULT).isEmpty());
    return this;
  }
}
