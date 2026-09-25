package com.mobile.swaglabs.qa.test;

import static com.mobile.swaglabs.qa.enums.UserPool.VALID_USERS_POOL;

import com.zebrunner.agent.core.annotation.TestCaseKey;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.mobile.swaglabs.qa.SwagLabsBaseTest;
import com.mobile.swaglabs.qa.enums.MenuItem;
import com.mobile.swaglabs.qa.pages.common.LoginPage;
import com.mobile.swaglabs.qa.pages.common.MenuPage;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.zebrunner.carina.core.registrar.ownership.MethodOwner;
import com.zebrunner.carina.core.registrar.tag.Priority;
import com.zebrunner.carina.core.registrar.tag.TestPriority;
import com.zebrunner.carina.core.registrar.tag.TestTag;

/** SL-14 .. SL-15 — the account menu and logout. */
public class AccountTest extends SwagLabsBaseTest {

    @Test(description = "Menu exposes all expected navigation items")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P2)
    @TestCaseKey(value = "SAUCEM-114")
    @TestTag(name = "tcId", value = "SL-14")
    @TestTag(name = "feature", value = "Account")
    public void testMenuItemsAreDisplayed() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        MenuPage menuPage = productsPage.openMenu();
        Assert.assertTrue(menuPage.isPageOpened(DEFAULT_TIMEOUT), MENU_NOT_OPENED);
        Assert.assertTrue(menuPage.isMenuOpened(), MENU_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        for (MenuItem item : MenuItem.values()) {
            softAssert.assertTrue(menuPage.isMenuItemDisplayed(item),
                    String.format("Menu entry '%s' is not displayed!", item.getLabel()));
        }

        softAssert.assertAll();

        ProductsPage reopenedProducts = menuPage.close();
        Assert.assertTrue(reopenedProducts.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);
    }

    @Test(description = "Logout returns to the login screen with fields cleared")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-115")
    @TestTag(name = "tcId", value = "SL-15")
    @TestTag(name = "feature", value = "Account")
    public void testLogoutReturnsToLoginPage() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        MenuPage menuPage = productsPage.openMenu();
        Assert.assertTrue(menuPage.isPageOpened(DEFAULT_TIMEOUT), MENU_NOT_OPENED);
        Assert.assertTrue(menuPage.isMenuItemDisplayed(MenuItem.LOGOUT),
                "LOGOUT entry is not displayed in the menu!");

        LoginPage loginPage = menuPage.logout();
        Assert.assertTrue(loginPage.isPageOpened(DEFAULT_TIMEOUT), LOGIN_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(loginPage.isUsernameFieldEmpty(),
                "The username field is not empty after logging out!");
        softAssert.assertTrue(loginPage.isPasswordFieldEmpty(),
                "The password field is not empty after logging out!");
        softAssert.assertTrue(loginPage.isErrorMessageAbsent(ONE),
                "An error banner is shown on the login page after a normal logout!");
        softAssert.assertAll();
    }
}
