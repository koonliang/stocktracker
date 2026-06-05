package com.stocktracker.e2e.support;

import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Captures a PNG at the point of failure so CI runs are easy to triage (FR-009).
 *
 * <p>Implemented as a {@link TestExecutionExceptionHandler} rather than a {@code TestWatcher}: this
 * callback fires <em>before</em> {@code @AfterEach}, so the failing test's WebDriver is still alive
 * when the screenshot is taken. The original exception is always rethrown so the test still fails.
 *
 * <p>Screenshots land in {@code e2e/target/screenshots/<TestClass>.<testMethod>.png} (uploaded as a
 * CI artifact) and are also attached to the Allure report so they render inline against the failing
 * test.
 */
public class ScreenshotOnFailure implements TestExecutionExceptionHandler {

  private static final Path SCREENSHOT_DIR = Path.of("target", "screenshots");

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
      throws Throwable {
    WebDriver driver = driverFrom(context);
    if (driver instanceof TakesScreenshot) {
      capture((TakesScreenshot) driver, context);
    }
    throw throwable;
  }

  private static WebDriver driverFrom(ExtensionContext context) {
    return context
        .getTestInstance()
        .filter(BaseTest.class::isInstance)
        .map(BaseTest.class::cast)
        .map(test -> test.driver)
        .orElse(null);
  }

  private static void capture(TakesScreenshot driver, ExtensionContext context) {
    try {
      byte[] png = driver.getScreenshotAs(OutputType.BYTES);
      String name = context.getRequiredTestClass().getSimpleName() + "." + context.getDisplayName();
      Files.createDirectories(SCREENSHOT_DIR);
      Files.write(SCREENSHOT_DIR.resolve(sanitize(name) + ".png"), png);
      // Attach to the Allure report so the screenshot shows inline on the failing test.
      Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");
    } catch (Exception e) {
      // Screenshot capture is best-effort diagnostics; never mask the real test failure.
      System.err.println("Failed to capture failure screenshot: " + e.getMessage());
    }
  }

  /** Strips characters that are illegal in filenames (e.g. {@code ()} from JUnit display names). */
  private static String sanitize(String name) {
    return name.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
