package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.DriverFactory;
import com.stocktracker.e2e.support.Waits;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Transactions ledger at route {@code /transactions}: CSV import (preview + commit) and export. */
public class TransactionsPage {

  private static final By NEW_TRANSACTION_BUTTON =
      By.xpath("//button[contains(normalize-space(.), 'New Transaction')]");
  private static final By NEW_TRANSACTION_FAB =
      By.cssSelector("[data-testid='fab'][aria-label='New transaction']");
  private static final By IMPORT_BUTTON =
      By.xpath("//button[contains(normalize-space(.), 'Import CSV')]");
  private static final By IMPORT_INPUT = By.cssSelector("[data-testid='csv-import-input']");
  private static final By EXPORT_BUTTON = By.cssSelector("[data-testid='csv-export']");
  private static final By TRANSACTIONS_TABLE = By.cssSelector("[data-testid='transactions-table']");
  private static final By TRANSACTION_FORM = By.cssSelector("[data-testid='transaction-form']");
  private static final By TRANSACTION_SUBMIT =
      By.cssSelector("button[form='transaction-create-form']");
  private static final By TICKER_SEARCH = By.cssSelector("[data-testid='transaction-ticker-search']");
  private static final By TICKER_RESULTS = By.cssSelector("[data-testid='transaction-ticker-result']");
  private static final By CONFIRM_IMPORT =
      By.xpath("//button[contains(normalize-space(.), 'Confirm import')]");

  private final WebDriver driver;
  private final Waits waits;

  public TransactionsPage(WebDriver driver, Waits waits) {
    this.driver = driver;
    this.waits = waits;
  }

  /** Uploads the CSV (file inputs accept sendKeys even while visually hidden), then commits it. */
  public TransactionsPage importCsv(Path csv) {
    openImportDialog();
    waits.untilVisible(IMPORT_INPUT).sendKeys(csv.toAbsolutePath().toString());
    waits.untilClickable(CONFIRM_IMPORT).click();
    return this;
  }

  /** Waits for a committed transaction row with the given ticker to appear in the ledger. */
  public void waitForTicker(String ticker) {
    waits.untilVisible(
        By.xpath(
            "//*[@data-testid='transactions-table']//tbody//td[normalize-space(.)='"
                + ticker
                + "']"));
  }

  public TransactionsPage recordBuy(String ticker, String quantity, String price) {
    waitForForm();
    selectTransactionType("buy");
    selectTicker(ticker);
    set(By.id("transaction-quantity"), quantity);
    set(By.id("transaction-price"), price);
    waits.untilClickable(TRANSACTION_SUBMIT).click();
    waits.untilInvisible(TRANSACTION_FORM);
    waitForTransactionRow(ticker, "buy");
    return this;
  }

  public TransactionsPage recordSplit(String ticker, String ratio) {
    waitForForm();
    selectTransactionType("split");
    selectTicker(ticker);
    set(By.id("transaction-quantity"), ratio);
    waits.untilClickable(TRANSACTION_SUBMIT).click();
    waits.untilInvisible(TRANSACTION_FORM);
    waitForTransactionRow(ticker, "split");
    return this;
  }

  public void waitForType(String type) {
    waits.untilVisible(
        By.xpath(
            "//*[@data-testid='transactions-table']//tbody//td//*[normalize-space(.)='"
                + type
                + "']"));
  }

  public void waitForTransactionRow(String ticker, String type) {
    waits.untilVisible(
        By.xpath(
            "//*[@data-testid='transactions-table']//tbody//tr[.//td[normalize-space(.)='"
                + ticker
                + "'] and .//td//*[normalize-space(.)='"
                + type
                + "']]"));
  }

  public TransactionsPage export() {
    waits.untilClickable(EXPORT_BUTTON).click();
    return this;
  }

  /** Waits until the browser has finished writing an exported CSV to the download directory. */
  public boolean waitForExportedCsv() {
    return waits.untilTrue(
        d -> {
          File[] files = DriverFactory.DOWNLOAD_DIR.listFiles();
          if (files == null) {
            return false;
          }
          for (File f : files) {
            if (f.getName().endsWith(".csv")) {
              return true;
            }
          }
          return false;
        });
  }

  public boolean isTableVisible() {
    return !driver.findElements(TRANSACTIONS_TABLE).isEmpty();
  }

  private void waitForForm() {
    openTransactionDialog();
    waits.untilVisible(TRANSACTION_FORM);
  }

  private void openTransactionDialog() {
    List<WebElement> existingForms = driver.findElements(TRANSACTION_FORM);
    if (!existingForms.isEmpty() && existingForms.get(0).isDisplayed()) {
      return;
    }
    clickVisibleLauncher(NEW_TRANSACTION_BUTTON, NEW_TRANSACTION_FAB);
    waits.untilVisible(TRANSACTION_FORM);
  }

  private void openImportDialog() {
    if (!driver.findElements(IMPORT_INPUT).isEmpty()) {
      return;
    }
    waits.untilClickable(IMPORT_BUTTON).click();
  }

  private void selectTicker(String ticker) {
    set(TICKER_SEARCH, ticker);
    waits.untilClickable(
            By.xpath(
                "//*[@data-testid='transaction-ticker-result'][.//*[normalize-space(.)='"
                    + ticker
                    + "']]"))
        .click();
    waits.untilInvisible(TICKER_RESULTS);
  }

  private void set(By locator, String value) {
    WebElement element = waits.untilVisible(locator);
    element.clear();
    element.sendKeys(value);
  }

  private void clickVisibleLauncher(By primary, By fallback) {
    for (WebElement button : driver.findElements(primary)) {
      if (button.isDisplayed() && button.isEnabled()) {
        button.click();
        return;
      }
    }
    for (WebElement button : driver.findElements(fallback)) {
      if (button.isDisplayed() && button.isEnabled()) {
        button.click();
        return;
      }
    }
    waits.untilClickable(primary).click();
  }

  private void selectTransactionType(String value) {
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        WebElement selectElement = waits.untilVisible(By.id("transaction-type"));
        new Select(selectElement).selectByValue(value);
        return;
      } catch (StaleElementReferenceException ignored) {
        // The dialog can rerender immediately after opening; refetch the select and retry.
      }
    }
    WebElement selectElement = waits.untilVisible(By.id("transaction-type"));
    new Select(selectElement).selectByValue(value);
  }
}
