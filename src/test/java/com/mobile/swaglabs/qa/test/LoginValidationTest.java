package com.mobile.swaglabs.qa.test;

import static com.mobile.swaglabs.qa.enums.UserPool.ALL_VALID_USERS_POOL;
import static com.mobile.swaglabs.qa.enums.UserPool.INVALID_USERS_POOL;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.mobile.swaglabs.qa.SwagLabsBaseTest;
import com.mobile.swaglabs.qa.data.UserData;
import com.mobile.swaglabs.qa.pages.common.LoginPage;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.mobile.swaglabs.qa.service.LoginAttempt;
import com.mobile.swaglabs.qa.service.UsersPool;
import com.zebrunner.carina.core.registrar.ownership.MethodOwner;
import com.zebrunner.carina.core.registrar.tag.Priority;
import com.zebrunner.carina.core.registrar.tag.TestPriority;
import com.zebrunner.carina.core.registrar.tag.TestTag;

/** SL-16 .. SL-17 — login validation. */
public class LoginValidationTest extends SwagLabsBaseTest {

    /**
     * Every user configured in {@code all_valid_users_pool}.
     *
     * <p>Reads the pool rather than leasing from it, so enumerating the accounts does not drain
     * the queue other tests lease from.
     */
    @DataProvider(name = "allValidUsers")
    public Object[][] allValidUsers() {
        return UsersPool.getInstance().getAllUsers(ALL_VALID_USERS_POOL).stream()
                .map(user -> new Object[] { user })
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "allValidUsers",
            description = "Every valid user in the pool can log in and reach Products")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestTag(name = "tcId", value = "SL-16")
    @TestTag(name = "feature", value = "Login")
    public void testLoginWithAllValidUsers(UserData user) {
        LOGGER.info("Verifying login for '{}'.", user.getLogin());

        ProductsPage productsPage = getLoginService().login(user);
        Assert.assertTrue(productsPage.isPageOpened(LOGIN_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(productsPage.getAllProducts().size(), EXPECTED_PRODUCT_COUNT,
                String.format("%s Seen while logged in as '%s'.", PRODUCT_COUNT_MISMATCH, user.getLogin()));

        // Carina recycles the driver per test method, not per data-provider invocation, so this
        // logout is what makes the next user's login genuinely independent.
        softAssert.assertAll();

        LoginPage loginPage = productsPage.openMenu().logout();
        Assert.assertTrue(loginPage.isPageOpened(DEFAULT_TIMEOUT), LOGIN_PAGE_NOT_OPENED);
        Assert.assertTrue(loginPage.isErrorMessageAbsent(ONE),
                String.format("An error banner is shown after '%s' logged out!", user.getLogin()));
    }

    @Test(description = "Invalid username is rejected with field and banner errors")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestTag(name = "tcId", value = "SL-17")
    @TestTag(name = "feature", value = "Login")
    public void testLoginWithInvalidUsernameIsRejected() {
        // Deliberate exception to the project rule that every test hard-asserts the Products page
        // OPENED: this case exists to prove the opposite. Please do not "normalise" it.
        LoginAttempt attempt = getLoginService().loginExpectingFailure(INVALID_USERS_POOL);

        Assert.assertFalse(attempt.getProductsPage().isPageOpened(), "The products page should not be open!");

        LoginPage loginPage = attempt.getLoginPage();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(loginPage.isErrorMessageDisplayed(DEFAULT_TIMEOUT),
                "The login error banner is not displayed!");
        softAssert.assertEquals(loginPage.getErrorMessageText(), LOGIN_ERROR_MESSAGE,
                "The login error banner text is incorrect!");
        softAssert.assertTrue(loginPage.isUsernameErrorIconDisplayed(),
                "The cross error icon is not shown in the username field!");
        softAssert.assertTrue(loginPage.isPasswordErrorIconDisplayed(),
                "The cross error icon is not shown in the password field!");
        softAssert.assertTrue(loginPage.isUsernameFieldBorderRed(),
                "The username field border did not turn red!");
        softAssert.assertTrue(loginPage.isPasswordFieldBorderRed(),
                "The password field border did not turn red!");
        softAssert.assertAll();
    }
}
