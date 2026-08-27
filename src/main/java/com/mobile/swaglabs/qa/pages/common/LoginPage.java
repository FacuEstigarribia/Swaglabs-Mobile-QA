package com.mobile.swaglabs.qa.pages.common;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.data.UserData;
import com.mobile.swaglabs.qa.util.ColorUtil;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

/**
 * The Swag Labs sign-in screen.
 */
public abstract class LoginPage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-Username")
    protected ExtendedWebElement fieldUsername;

    @ExtendedFindBy(accessibilityId = "test-Password")
    protected ExtendedWebElement fieldPassword;

    @ExtendedFindBy(accessibilityId = "test-LOGIN")
    protected ExtendedWebElement buttonLogin;

    @ExtendedFindBy(accessibilityId = "test-Error message")
    protected ExtendedWebElement labelErrorMessage;

    public LoginPage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(fieldUsername);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    /**
     * Submits the given credentials.
     */
    public ProductsPage login(UserData user) {
        LOGGER.info("Logging in as '{}'.", user.getLogin());
        typeUsername(user.getLogin());
        typePassword(user.getPassword());
        tap(buttonLogin);
        return initPage(getDriver(), ProductsPage.class);
    }

    public void typeUsername(String username) {
        fieldUsername.click();
        fieldUsername.type(username);
    }

    public void typePassword(String password) {
        fieldPassword.click();
        fieldPassword.type(password);
    }

    public boolean isUsernameFieldEmpty() {
        return isPlaceholder(fieldUsername.getText(), "Username");
    }

    public boolean isPasswordFieldEmpty() {
        return isPlaceholder(fieldPassword.getText(), "Password");
    }

    /**
     * The app renders its placeholder as the field's own text, so an empty field reads back as
     * the placeholder rather than as "".
     */
    private boolean isPlaceholder(String actual, String placeholder) {
        return actual == null || actual.trim().isEmpty() || placeholder.equalsIgnoreCase(actual.trim());
    }

    public boolean isErrorMessageDisplayed(long timeout) {
        return labelErrorMessage.isElementPresent(timeout);
    }

    public boolean isErrorMessageAbsent(long timeout) {
        return labelErrorMessage.isElementNotPresent(timeout);
    }

    public abstract String getErrorMessageText();

    public abstract boolean isUsernameErrorIconDisplayed();

    public abstract boolean isPasswordErrorIconDisplayed();

    public boolean isUsernameFieldBorderRed() {
        return ColorUtil.hasRedBorderBelow(getDriver(), fieldUsername);
    }
    public boolean isPasswordFieldBorderRed() {
        return ColorUtil.hasRedBorderBelow(getDriver(), fieldPassword);
    }
}
