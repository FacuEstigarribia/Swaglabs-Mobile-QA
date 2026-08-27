package com.mobile.swaglabs.qa;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

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

    /**
     * Loads the capabilities for the platform this suite targets.
     *
     * <p>Carina does not read {@code capabilities.*} out of TestNG suite parameters, so each suite
     * instead names a platform and the matching properties file is loaded into {@code R.CONFIG}
     * here. Keeping the two platforms in separate files is also what lets one set of test classes
     * run unchanged against both.
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

    /** A login service bound to the current thread's driver. */
    public LoginService getLoginService() {
        return new LoginService(getDriver());
    }

    /**
     * Returns any user this test leased.
     *
     * <p>The driver itself is disposed by Carina's {@code method_mode}, which gives the next test
     * a freshly launched app and therefore a clean cart and session.
     */
    @AfterMethod(alwaysRun = true)
    public void releaseUsers() {
        UsersPool.getInstance().releaseCurrentUsers();
    }
}
