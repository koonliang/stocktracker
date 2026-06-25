package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.Waits;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Watchlists list ({@code /watchlists}) and detail ({@code /watchlists/:id}). Covers creating a
 * watchlist and adding/removing an instrument.
 */
public class WatchlistsPage {

  private static final By NEW_WATCHLIST_BUTTON =
      By.xpath("//button[contains(normalize-space(.), 'New Watchlist')]");
  private static final By NAME_INPUT = By.id("new-watchlist-name");
  private static final By CREATE_BUTTON = By.xpath("//button[normalize-space(.)='Create']");
  private static final By SYMBOL_SEARCH_INPUT = By.cssSelector("[data-testid='symbol-search']");

  private final WebDriver driver;
  private final Waits waits;

  public WatchlistsPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  /** Creates a new watchlist via the dialog; the app then navigates to its detail view. */
  public WatchlistsPage createWatchlist(String name) {
    waits.untilClickable(NEW_WATCHLIST_BUTTON).click();
    waits.untilVisible(NAME_INPUT).sendKeys(name);
    waits.untilClickable(CREATE_BUTTON).click();
    // Detail view shows the symbol search control once navigation completes.
    waits.untilVisible(SYMBOL_SEARCH_INPUT);
    return this;
  }

  public WatchlistsPage addTicker(String symbol) {
    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    WebElement input = waits.untilVisible(SYMBOL_SEARCH_INPUT);
    input.clear();
    input.sendKeys(normalizedSymbol);
    waits.untilClickable(addButtonLocator(normalizedSymbol)).click();
    return this;
  }

  public WebElement waitForItem(String symbol) {
    return waits.untilVisible(itemLocator(symbol));
  }

  public WatchlistsPage removeItem(String symbol) {
    WebElement item = waitForItem(symbol);
    item.findElement(By.cssSelector("[data-testid='watchlist-remove']")).click();
    return this;
  }

  public boolean waitForItemGone(String symbol) {
    return waits.untilInvisible(itemLocator(symbol));
  }

  private static By itemLocator(String symbol) {
    return By.cssSelector("[data-testid='watchlist-item-" + symbol + "']");
  }

  private static By addButtonLocator(String symbol) {
    return By.cssSelector("button[aria-label='Add " + symbol + "']");
  }
}
