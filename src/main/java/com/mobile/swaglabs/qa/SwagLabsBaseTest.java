package com.mobile.swaglabs.qa;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import com.mobile.swaglabs.qa.retry.RetryCountAnalyzer;
import com.mobile.swaglabs.qa.service.LoginService;
import com.mobile.swaglabs.qa.service.UsersPool;
import com.zebrunner.carina.core.AbstractTest;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.webdriver.core.capability.CapabilitiesLoader;

/**
 * Base class for every test.
 */
public class SwagLabsBaseTest extends AbstractTest implements IConstants {

    protected static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CAPABILITIES_PATH = "capabilities/%s.properties";
    private static final String CAPABILITY_PREFIX = "capabilities.";

    private static final Path ALLURE_RESULTS = Paths.get("target", "allure-results");

    /**
     * Loads the capabilities for the platform this suite targets.
     */
    @BeforeSuite(alwaysRun = true)
    @Parameters({ "platform" })
    public void loadPlatformCapabilities(String platform) {
        String path = String.format(CAPABILITIES_PATH, platform.toLowerCase());
        LOGGER.info("Loading {} capabilities from '{}'.", platform, path);
        new CapabilitiesLoader().loadCapabilities(path);
        applyCommandLineOverrides();
    }

    /**
     * Re-applies any {@code -Dcapabilities.*} given on the command line.
     */
    private void applyCommandLineOverrides() {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.startsWith(CAPABILITY_PREFIX)) {
                LOGGER.info("Overriding '{}' with the value given on the command line.", key);
                R.CONFIG.put(key, String.valueOf(entry.getValue()));
            }
        }
    }

    /**
     * Records the run's environment for the Allure report.
     */
    @BeforeSuite(alwaysRun = true, dependsOnMethods = "loadPlatformCapabilities")
    public void writeAllureEnvironment(ITestContext context) {
        try {
            Files.createDirectories(ALLURE_RESULTS);
            writeEnvironment(context);
        } catch (Exception e) {
            // Reporting must never take a suite down before it has run a single test.
            LOGGER.warn("Could not write the Allure environment file.", e);
        }
    }

    private void writeEnvironment(ITestContext context) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Platform", R.CONFIG.get("capabilities.platformName"));
        values.put("Device", R.CONFIG.get("capabilities.deviceName"));
        values.put("Platform.version", R.CONFIG.get("capabilities.platformVersion"));
        values.put("Automation", R.CONFIG.get("capabilities.automationName"));
        values.put("App", app());
        values.put("Appium", R.CONFIG.get("selenium_url"));
        values.put("Suite", context.getSuite().getName());
        values.put("Retry.count", String.valueOf(RetryCountAnalyzer.resolveMaxRetries(context)));

        Properties properties = new Properties();
        values.forEach((key, value) -> {
            if (value != null && !value.isEmpty() && !"NULL".equalsIgnoreCase(value)) {
                properties.setProperty(key, value);
            }
        });

        try (OutputStream out = Files.newOutputStream(ALLURE_RESULTS.resolve("environment.properties"))) {
            properties.store(out, "Swag Labs mobile run");
        }
        LOGGER.info("Allure environment written for the {} suite.", context.getSuite().getName());
    }

    /** Android by app package, iOS by bundle id. */
    private String app() {
        String appPackage = R.CONFIG.get("capabilities.appPackage");
        return appPackage == null || appPackage.isEmpty() || "NULL".equalsIgnoreCase(appPackage)
                ? R.CONFIG.get("capabilities.bundleId")
                : appPackage;
    }

    /** A login service bound to the current thread's driver. */
    public LoginService getLoginService() {
        return new LoginService(getDriver());
    }

    /**
     * Returns any user this test leased.
     */
    @AfterMethod(alwaysRun = true)
    public void releaseUsers() {
        UsersPool.getInstance().releaseCurrentUsers();
    }
}
