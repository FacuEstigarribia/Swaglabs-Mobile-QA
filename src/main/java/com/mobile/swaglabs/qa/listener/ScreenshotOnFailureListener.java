package com.mobile.swaglabs.qa.listener;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.zebrunner.carina.webdriver.IDriverPool;
import com.zebrunner.carina.webdriver.Screenshot;
import com.zebrunner.carina.webdriver.ScreenshotType;

/**
 * Captures a screenshot whenever a test fails.
 *
 * <p>Registered per suite in {@code src/test/resources/testng_suites/*.xml}.
 *
 * <p>Two captures, deliberately. Carina's {@link Screenshot} feeds the framework report and honours
 * its screenshot rules; the explicit file write guarantees a {@code .png} on disk under
 * {@code target/screenshots/} regardless of how reporting is configured.
 *
 * <p>Implementing {@link IDriverPool} supplies {@code getDriver()}, which resolves the driver of
 * the current thread — listener callbacks run on the test's own thread.
 *
 * <p>Every failure path is caught and logged: a screenshot problem must never replace the real
 * test failure with an unrelated error.
 */
public class ScreenshotOnFailureListener implements ITestListener, IDriverPool {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final Path SCREENSHOT_DIR = Paths.get("target", "screenshots");

    @Override
    public void onTestFailure(ITestResult result) {
        String name = buildName(result);
        LOGGER.info("Test '{}' failed; capturing a screenshot.", name);
        try {
            WebDriver driver = getDriver();
            Screenshot.capture(driver, ScreenshotType.EXPLICIT_FULL_SIZE, name)
                    .ifPresent(path -> LOGGER.info("Failure screenshot added to the report: {}", path));
            saveToDisk(driver, name);
        } catch (Exception e) {
            LOGGER.warn("Could not capture a failure screenshot for '{}'.", name, e);
        }
    }

    private void saveToDisk(WebDriver driver, String name) throws IOException {
        Files.createDirectories(SCREENSHOT_DIR);
        Path target = SCREENSHOT_DIR.resolve(name + ".png");
        Files.write(target, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        LOGGER.info("Failure screenshot saved: {}", target.toAbsolutePath());
    }

    /** A filesystem-safe name identifying the failing class and method. */
    private String buildName(ITestResult result) {
        String raw = String.format("FAILURE_%s.%s",
                result.getTestClass().getRealClass().getSimpleName(), result.getName());
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
