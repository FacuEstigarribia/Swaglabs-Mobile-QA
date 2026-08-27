package com.mobile.swaglabs.qa.pages.common;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.enums.MenuItem;
import io.appium.java_client.AppiumBy;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

/**
 * The hamburger navigation drawer.
 */
public abstract class MenuPage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-Close")
    protected ExtendedWebElement buttonClose;

    @ExtendedFindBy(accessibilityId = "test-ALL ITEMS")
    protected ExtendedWebElement itemAllItems;

    @ExtendedFindBy(accessibilityId = "test-LOGOUT")
    protected ExtendedWebElement itemLogout;

    @ExtendedFindBy(accessibilityId = "test-RESET APP STATE")
    protected ExtendedWebElement itemResetAppState;

    protected MenuPage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(itemAllItems);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    /** Whether the drawer is open, i.e. its entries are on screen rather than parked off it. */
    public boolean isMenuOpened() {
        return isOnScreen(itemAllItems);
    }

    /** Whether {@code item} is present and within the viewport. */
    public boolean isMenuItemDisplayed(MenuItem item) {
        ExtendedWebElement element =
                findExtendedWebElement(AppiumBy.accessibilityId(item.getAccessibilityId()), ONE);
        return element != null && isOnScreen(element);
    }

    /** True when the element's left edge is inside the viewport. */
    protected boolean isOnScreen(ExtendedWebElement element) {
        return element.isElementPresent(ONE) && element.getRect().getX() >= ZERO;
    }

    public void selectMenuItem(MenuItem item) {
        LOGGER.info("Selecting menu item '{}'.", item.getLabel());
        tap(findExtendedWebElement(AppiumBy.accessibilityId(item.getAccessibilityId()), DEFAULT_TIMEOUT));
    }

    public ProductsPage selectAllItems() {
        tap(itemAllItems);
        return initPage(getDriver(), ProductsPage.class);
    }

    public LoginPage logout() {
        LOGGER.info("Logging out.");
        tap(itemLogout);
        return initPage(getDriver(), LoginPage.class);
    }

    /** Clears the cart and the add-to-cart state without restarting the app. */
    public void resetAppState() {
        LOGGER.info("Resetting app state.");
        tap(itemResetAppState);
    }

    public ProductsPage close() {
        tap(buttonClose);
        return initPage(getDriver(), ProductsPage.class);
    }
}
