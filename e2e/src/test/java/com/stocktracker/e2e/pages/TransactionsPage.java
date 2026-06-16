package com.stocktracker.e2e.pages;

import com.stocktracker.e2e.support.DriverFactory;
import com.stocktracker.e2e.support.Waits;
import java.io.File;
import java.nio.file.Path;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Transactions ledger at route {@code /transactions}: CSV import (preview + commit) and export. */
public class TransactionsPage {

  private static final By IMPORT_INPUT = By.cssSelector("[data-testid='csv-import-input']");
  private static final By EXPORT_BUTTON = By.cssSelector("[data-testid='csv-export']");
  private static final By TRANSACTIONS_TABLE = By.cssSelector("[data-testid='transactions-table']");
  private static final By TRANSACTION_FORM = By.cssSelector("[data-testid='transaction-form']");
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
    // The file input is present from load; sendKeys triggers the preview.
    driver.findElement(IMPORT_INPUT).sendKeys(csv.toAbsolutePath().toString());
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
    new Select(driver.findElement(By.id("transaction-type"))).selectByValue("buy");
    set(By.id("transaction-ticker"), ticker);
    set(By.id("transaction-quantity"), quantity);
    set(By.id("transaction-price"), price);
    driver.findElement(By.cssSelector("[data-testid='transaction-form'] button[type='submit']")).click();
    waitForTicker(ticker);
    return this;
  }

  public TransactionsPage recordSplit(String ticker, String ratio) {
    waitForForm();
    new Select(driver.findElement(By.id("transaction-type"))).selectByValue("split");
    set(By.id("transaction-ticker"), ticker);
    set(By.id("transaction-quantity"), ratio);
    driver.findElement(By.cssSelector("[data-testid='transaction-form'] button[type='submit']")).click();
    waitForType("split");
    return this;
  }

  public void waitForType(String type) {
    waits.untilVisible(
        By.xpath(
            "//*[@data-testid='transactions-table']//tbody//td//*[normalize-space(.)='"
                + type
                + "']"));
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
    waits.untilVisible(TRANSACTION_FORM);
  }

  private void set(By locator, String value) {
    WebElement element = waits.untilVisible(locator);
    element.clear();
    element.sendKeys(value);
  }
}
