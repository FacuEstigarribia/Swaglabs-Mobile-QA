package com.mobile.swaglabs.qa.service;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

import com.mobile.swaglabs.qa.IConstants;
import com.mobile.swaglabs.qa.data.UserData;
import com.mobile.swaglabs.qa.data.UserProvider;
import com.mobile.swaglabs.qa.pages.common.LoginPage;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.zebrunner.carina.utils.factory.ICustomTypePageFactory;
import com.zebrunner.carina.webdriver.IDriverPool;

/**
 * Authentication workflow.
 */
public class LoginService implements IDriverPool, ICustomTypePageFactory, IConstants {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private final WebDriver driver;

    public LoginService(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Logs in with a user from {@code provider}.
     *
     * <p>Accepts a {@code UserPool} constant directly, so a test reads
     * {@code getLoginService().login(VALID_USERS_POOL)}.
     */
    public ProductsPage login(UserProvider provider) {
        return login(provider.getUser());
    }

    /** Logs in as {@code user} and returns the Products page. */
    public ProductsPage login(UserData user) {
        LoginPage loginPage = openLoginPage();
        return loginPage.login(user);
    }

    /**
     * Submits credentials that are expected to be rejected.
     */
    public LoginAttempt loginExpectingFailure(UserProvider provider) {
        UserData user = provider.getUser();
        LOGGER.info("Attempting a login expected to fail, as '{}'.", user.getLogin());
        LoginPage loginPage = openLoginPage();
        ProductsPage productsPage = loginPage.login(user);
        return new LoginAttempt(loginPage, productsPage);
    }

    private LoginPage openLoginPage() {
        LoginPage loginPage = initPage(driver, LoginPage.class);
        Assert.assertTrue(loginPage.isPageOpened(LOGIN_TIMEOUT), LOGIN_PAGE_NOT_OPENED);
        return loginPage;
    }
}
