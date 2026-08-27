package com.mobile.swaglabs.qa.test;

import static com.mobile.swaglabs.qa.enums.UserPool.VALID_USERS_POOL;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.mobile.swaglabs.qa.SwagLabsBaseTest;
import com.mobile.swaglabs.qa.data.Product;
import com.mobile.swaglabs.qa.pages.common.CartPage;
import com.mobile.swaglabs.qa.pages.common.ProductDetailsPage;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.zebrunner.carina.core.registrar.ownership.MethodOwner;
import com.zebrunner.carina.core.registrar.tag.Priority;
import com.zebrunner.carina.core.registrar.tag.TestPriority;
import com.zebrunner.carina.core.registrar.tag.TestTag;

/** SL-09 .. SL-13 — the cart. */
public class CartTest extends SwagLabsBaseTest {

    @Test(description = "Add a single item from the grid updates the cart badge")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
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

    private List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).collect(Collectors.toList());
    }
}
