package com.stocktracker.e2e.support;

import java.io.File;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Builds the headless Chrome {@link WebDriver} used by every journey. Selenium Manager (built into
 * Selenium 4.6+) resolves the matching ChromeDriver automatically, so no driver download is needed.
 *
 * <p>Headless is the default; pass {@code -De2e.headless=false} to watch the browser locally while
 * debugging.
 */
public final class DriverFactory {

  /** Where the browser drops downloaded files (under the module's target/, gitignored). */
  public static final File DOWNLOAD_DIR = new File("target/downloads").getAbsoluteFile();

  private DriverFactory() {}

  public static WebDriver create() {
    DOWNLOAD_DIR.mkdirs();
    ChromeOptions options = new ChromeOptions();
    if (headless()) {
      options.addArguments("--headless=new");
    }
    options.addArguments(
        "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--window-size=1920,1080");
    // Headless download prefs so the CSV-export journey can assert a file lands (no prompt).
    options.setExperimentalOption(
        "prefs",
        Map.of(
            "download.default_directory",
            DOWNLOAD_DIR.getAbsolutePath(),
            "download.prompt_for_download",
            false));
    return new ChromeDriver(options);
  }

  private static boolean headless() {
    return !"false".equalsIgnoreCase(System.getProperty("e2e.headless", "true"));
  }
}
