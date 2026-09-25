package com.mobile.swaglabs.qa.test;

import static com.mobile.swaglabs.qa.enums.UserPool.VALID_USERS_POOL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import com.zebrunner.agent.core.annotation.TestCaseKey;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.mobile.swaglabs.qa.SwagLabsBaseTest;
import com.mobile.swaglabs.qa.data.Product;
import com.mobile.swaglabs.qa.pages.common.CartPage;
import com.mobile.swaglabs.qa.pages.common.CheckoutCompletePage;
import com.mobile.swaglabs.qa.pages.common.CheckoutInformationPage;
import com.mobile.swaglabs.qa.pages.common.CheckoutOverviewPage;
import com.mobile.swaglabs.qa.pages.common.ProductDetailsPage;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.mobile.swaglabs.qa.service.CheckoutService;
import com.zebrunner.carina.core.registrar.ownership.MethodOwner;
import com.zebrunner.carina.core.registrar.tag.Priority;
import com.zebrunner.carina.core.registrar.tag.TestPriority;
import com.zebrunner.carina.core.registrar.tag.TestTag;

/** SL-09 .. SL-13 — the cart. */
public class CartTest extends SwagLabsBaseTest {

    @Test(description = "Add a single item from the grid updates the cart badge")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-109")
    @TestTag(name = "tcId", value = "SL-09")
    @TestTag(name = "feature", value = "Cart")
    public void testAddSingleItemFromGrid() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(productsPage.getCartBadgeCount(), ZERO,
                "The cart badge is showing before anything was added!");

        Product added = productsPage.addProductToCart(ZERO);
        softAssert.assertTrue(productsPage.isRemoveButtonDisplayedFor(added),
                "ADD TO CART did not change to REMOVE!");
        softAssert.assertEquals(productsPage.getCartBadgeCount(), ONE, CART_BADGE_MISMATCH);

        CartPage cartPage = productsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);

        List<Product> cartItems = cartPage.getItems();
        softAssert.assertEquals(cartItems.size(), ONE, CART_ITEM_COUNT_MISMATCH);
        if (!cartItems.isEmpty()) {
            softAssert.assertEquals(cartItems.get(ZERO).getName(), added.getName(),
                    "The cart line item is not the product that was added!");
            softAssert.assertEquals(cartItems.get(ZERO).getPriceLabel(), added.getPriceLabel(),
                    "The cart line item price does not match the product added!");
        }
        softAssert.assertAll();
    }

    @Test(description = "Add an item from the product details page")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-110")
    @TestTag(name = "tcId", value = "SL-10")
    @TestTag(name = "feature", value = "Cart")
    public void testAddItemFromProductDetails() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        ProductDetailsPage detailsPage = productsPage.openProduct(ZERO);
        Assert.assertTrue(detailsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCT_DETAILS_PAGE_NOT_OPENED);

        Product fromDetails = detailsPage.toProduct();
        detailsPage.addToCart();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(detailsPage.isRemoveDisplayed(),
                "ADD TO CART did not change to REMOVE on the details page!");
        softAssert.assertEquals(detailsPage.getCartBadgeCount(), ONE, CART_BADGE_MISMATCH);

        CartPage cartPage = detailsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);

        List<Product> cartItems = cartPage.getItems();
        softAssert.assertEquals(cartItems.size(), ONE, CART_ITEM_COUNT_MISMATCH);
        if (!cartItems.isEmpty()) {
            softAssert.assertEquals(cartItems.get(ZERO).getName(), fromDetails.getName(),
                    "The cart line item is not the product added from the details page!");
            softAssert.assertEquals(cartItems.get(ZERO).getPriceLabel(), fromDetails.getPriceLabel(),
                    "The cart line item price does not match the details page!");
        }
        softAssert.assertAll();
    }

    @Test(description = "Add multiple items and verify cart contents match the badge")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-111")
    @TestTag(name = "tcId", value = "SL-11")
    @TestTag(name = "feature", value = "Cart")
    public void testAddMultipleItemsToCart() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        List<Product> added = productsPage.addProductsToCart(THREE);
        softAssert.assertEquals(productsPage.getCartBadgeCount(), THREE, CART_BADGE_MISMATCH);

        CartPage cartPage = productsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);

        softAssert.assertEquals(cartPage.getItemCount(), THREE, CART_ITEM_COUNT_MISMATCH);
        softAssert.assertEquals(cartPage.getItemNames(), names(added),
                "The cart does not contain exactly the products that were added!");
        softAssert.assertAll();
    }

    @Test(description = "Remove an item from the cart")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-112")
    @TestTag(name = "tcId", value = "SL-12")
    @TestTag(name = "feature", value = "Cart")
    public void testRemoveItemFromCart() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        productsPage.addProductsToCart(TWO);
        softAssert.assertEquals(productsPage.getCartBadgeCount(), TWO, CART_BADGE_MISMATCH);

        CartPage cartPage = productsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);
        softAssert.assertEquals(cartPage.getItemCount(), TWO, CART_ITEM_COUNT_MISMATCH);

        cartPage.removeItem(ZERO);
        softAssert.assertEquals(cartPage.getItemCount(), ONE,
                "Removing a line item did not leave exactly one item!");
        softAssert.assertEquals(cartPage.getCartBadgeCount(), ONE, CART_BADGE_MISMATCH);

        cartPage.removeItem(ZERO);
        softAssert.assertTrue(cartPage.isEmpty(), "The cart is not empty after removing both items!");
        softAssert.assertEquals(cartPage.getCartBadgeCount(), ZERO,
                "The cart badge is still showing after emptying the cart!");
        softAssert.assertAll();
    }

    @Test(description = "Cart contents survive Continue Shopping")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P2)
    @TestCaseKey(value = "SAUCEM-113")
    @TestTag(name = "tcId", value = "SL-13")
    @TestTag(name = "feature", value = "Cart")
    public void testCartPersistsAfterContinueShopping() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        productsPage.addProductsToCart(TWO);
        softAssert.assertEquals(productsPage.getCartBadgeCount(), TWO, CART_BADGE_MISMATCH);

        CartPage cartPage = productsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);
        List<String> beforeLeaving = cartPage.getItemNames();

        ProductsPage returnedPage = cartPage.continueShopping();
        Assert.assertTrue(returnedPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);
        softAssert.assertEquals(returnedPage.getCartBadgeCount(), TWO,
                "The cart badge changed after continuing shopping!");

        CartPage reopenedCart = returnedPage.openCart();
        Assert.assertTrue(reopenedCart.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);
        softAssert.assertEquals(reopenedCart.getItemNames(), beforeLeaving,
                "The cart contents changed after continuing shopping!");
        softAssert.assertAll();
    }

    @Test(description = "Complete checkout with totals calculated from displayed product prices")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-118")
    @TestTag(name = "tcId", value = "SL-18")
    @TestTag(name = "feature", value = "Checkout")
    public void testSuccessfulCheckoutCalculatesTotalsAndCompletesPurchase() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        List<Product> added = productsPage.addProductsToCart(TWO);
        Assert.assertEquals(productsPage.getCartBadgeCount(), TWO, CART_BADGE_MISMATCH);

        CartPage cartPage = productsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);

        CheckoutService checkoutService = getCheckoutService();
        CheckoutInformationPage informationPage = checkoutService.startCheckout();
        CheckoutOverviewPage overviewPage = checkoutService.submitInformation(informationPage,
                CHECKOUT_FIRST_NAME, CHECKOUT_LAST_NAME, CHECKOUT_POSTAL_CODE);

        BigDecimal expectedSubtotal = subtotalOf(added);
        BigDecimal expectedTax = expectedSubtotal.multiply(new BigDecimal(CHECKOUT_TAX_RATE))
                .setScale(TWO, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = expectedSubtotal.add(expectedTax);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(overviewPage.getItems(), added,
                "Checkout overview items do not match the products collected from the UI!");
        softAssert.assertEquals(overviewPage.getSubtotal(), expectedSubtotal,
                "Checkout subtotal is not the sum of the displayed product prices!");
        softAssert.assertEquals(overviewPage.getTax(), expectedTax,
                "Checkout tax is not calculated from the displayed product prices!");
        softAssert.assertEquals(overviewPage.getTotal(), expectedTotal,
                "Checkout total is not the displayed subtotal plus calculated tax!");
        softAssert.assertAll();

        CheckoutCompletePage completePage = checkoutService.finishCheckout(overviewPage);
        Assert.assertTrue(completePage.isBackHomeDisplayed(),
                "BACK HOME is not displayed after completing checkout!");

        ProductsPage returnedPage = completePage.backHome();
        Assert.assertTrue(returnedPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);
        Assert.assertEquals(returnedPage.getCartBadgeCount(), ZERO,
                "The cart was not cleared after completing checkout!");
    }

    @Test(description = "Checkout requires a first name")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-119")
    @TestTag(name = "tcId", value = "SL-19")
    @TestTag(name = "feature", value = "Checkout")
    public void testCheckoutRequiresFirstName() {
        assertRequiredFieldValidation("", CHECKOUT_LAST_NAME, CHECKOUT_POSTAL_CODE,
                FIRST_NAME_REQUIRED_ERROR);
    }

    @Test(description = "Checkout requires a last name")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-120")
    @TestTag(name = "tcId", value = "SL-20")
    @TestTag(name = "feature", value = "Checkout")
    public void testCheckoutRequiresLastName() {
        assertRequiredFieldValidation(CHECKOUT_FIRST_NAME, "", CHECKOUT_POSTAL_CODE,
                LAST_NAME_REQUIRED_ERROR);
    }

    @Test(description = "Checkout requires a postal code")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-121")
    @TestTag(name = "tcId", value = "SL-21")
    @TestTag(name = "feature", value = "Checkout")
    public void testCheckoutRequiresPostalCode() {
        assertRequiredFieldValidation(CHECKOUT_FIRST_NAME, CHECKOUT_LAST_NAME, "",
                POSTAL_CODE_REQUIRED_ERROR);
    }

    @Test(description = "Cancel checkout returns to products and preserves the cart")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P2)
    @TestCaseKey(value = "SAUCEM-122")
    @TestTag(name = "tcId", value = "SL-22")
    @TestTag(name = "feature", value = "Checkout")
    public void testCheckoutCancelPreservesCart() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);
        List<Product> added = productsPage.addProductsToCart(ONE);

        CartPage cartPage = productsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);

        CheckoutService checkoutService = getCheckoutService();
        CheckoutInformationPage informationPage = checkoutService.startCheckout();
        ProductsPage returnedFromInformation = informationPage.cancel();
        Assert.assertTrue(returnedFromInformation.isPageOpened(DEFAULT_TIMEOUT),
                PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(returnedFromInformation.getCartBadgeCount(), ONE,
                "The cart badge changed after cancelling checkout information!");

        CartPage reopenedCart = returnedFromInformation.openCart();
        Assert.assertTrue(reopenedCart.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);
        softAssert.assertEquals(reopenedCart.getItems(), added,
                "The cart changed after cancelling checkout information!");

        CheckoutInformationPage reopenedInformation = checkoutService.startCheckout();
        CheckoutOverviewPage overviewPage = checkoutService.submitInformation(reopenedInformation,
                CHECKOUT_FIRST_NAME, CHECKOUT_LAST_NAME, CHECKOUT_POSTAL_CODE);
        ProductsPage returnedFromOverview = overviewPage.cancel();
        Assert.assertTrue(returnedFromOverview.isPageOpened(DEFAULT_TIMEOUT),
                PRODUCTS_PAGE_NOT_OPENED);
        softAssert.assertEquals(returnedFromOverview.getCartBadgeCount(), ONE,
                "The cart badge changed after cancelling checkout overview!");

        CartPage cartAfterOverview = returnedFromOverview.openCart();
        Assert.assertTrue(cartAfterOverview.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);
        softAssert.assertEquals(cartAfterOverview.getItems(), added,
                "The cart changed after cancelling checkout overview!");
        softAssert.assertAll();
    }

    private void assertRequiredFieldValidation(String firstName, String lastName,
            String postalCode, String expectedMessage) {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);
        productsPage.addProductToCart(ZERO);

        CartPage cartPage = productsPage.openCart();
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);

        CheckoutInformationPage informationPage = getCheckoutService().startCheckout();
        informationPage.fillInformation(firstName, lastName, postalCode);
        informationPage.submit();

        Assert.assertTrue(informationPage.isPageOpened(DEFAULT_TIMEOUT),
                CHECKOUT_INFORMATION_PAGE_NOT_OPENED);
        Assert.assertTrue(informationPage.isErrorMessageDisplayed(DEFAULT_TIMEOUT),
                "Required-field error message is not displayed!");
        Assert.assertEquals(informationPage.getErrorMessageText(), expectedMessage,
                "Required-field error message is incorrect!");
    }

    private BigDecimal subtotalOf(List<Product> products) {
        return products.stream()
                .map(product -> new BigDecimal(product.getPriceLabel()
                        .replace("$", "").replace(",", "").trim()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(TWO, RoundingMode.HALF_UP);
    }

    private List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).collect(Collectors.toList());
    }
}
