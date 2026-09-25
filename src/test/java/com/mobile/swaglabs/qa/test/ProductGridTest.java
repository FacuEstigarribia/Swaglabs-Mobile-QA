package com.mobile.swaglabs.qa.test;

import static com.mobile.swaglabs.qa.enums.UserPool.VALID_USERS_POOL;

import java.util.List;

import com.zebrunner.agent.core.annotation.TestCaseKey;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.mobile.swaglabs.qa.SwagLabsBaseTest;
import com.mobile.swaglabs.qa.components.common.ProductCard;
import com.mobile.swaglabs.qa.data.Product;
import com.mobile.swaglabs.qa.pages.common.ProductDetailsPage;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.zebrunner.carina.core.registrar.ownership.MethodOwner;
import com.zebrunner.carina.core.registrar.tag.Priority;
import com.zebrunner.carina.core.registrar.tag.TestPriority;
import com.zebrunner.carina.core.registrar.tag.TestTag;

/** SL-01 .. SL-04 — the product grid. */
public class ProductGridTest extends SwagLabsBaseTest {

    @Test(description = "Product grid shows all catalog items")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-101")
    @TestTag(name = "tcId", value = "SL-01")
    @TestTag(name = "feature", value = "Product Grid")
    public void testProductGridDisplaysAllItems() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(productsPage.isProductsTitleDisplayed(), "Products header is not displayed!");

        List<Product> products = productsPage.getAllProducts();
        softAssert.assertEquals(products.size(), EXPECTED_PRODUCT_COUNT, PRODUCT_COUNT_MISMATCH);
        for (Product product : products) {
            softAssert.assertFalse(product.getName().trim().isEmpty(),
                    "A product card shows an empty name!");
            softAssert.assertTrue(product.getPriceLabel().matches(PRICE_PATTERN),
                    String.format("Price '%s' of '%s' is not formatted as $X.XX!",
                            product.getPriceLabel(), product.getName()));
        }

        for (ProductCard card : productsPage.findRenderedCards()) {
            softAssert.assertTrue(card.isAddToCartDisplayed(),
                    String.format("Card '%s' has no ADD TO CART button!", card.getProductName()));
            softAssert.assertTrue(card.isPriceDisplayed(),
                    String.format("Card '%s' has no price!", card.getProductName()));
        }
        softAssert.assertAll();
    }

    @Test(description = "Grid and list view toggle changes layout and preserves items")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P2)
    @TestCaseKey(value = "SAUCEM-102")
    @TestTag(name = "tcId", value = "SL-02")
    @TestTag(name = "feature", value = "Product Grid")
    public void testToggleGridAndListView() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        List<String> beforeToggle = productsPage.getAllProductNames();
        softAssert.assertEquals(beforeToggle.size(), EXPECTED_PRODUCT_COUNT, PRODUCT_COUNT_MISMATCH);
        softAssert.assertTrue(productsPage.isViewToggleDisplayed(), "View toggle is not displayed!");

        productsPage.toggleView();
        List<String> afterToggle = productsPage.getAllProductNames();
        softAssert.assertEquals(afterToggle, beforeToggle,
                "Toggling the view changed the products or their order!");
        softAssert.assertTrue(productsPage.isViewToggleDisplayed(),
                "View toggle disappeared after toggling!");

        productsPage.toggleView();
        softAssert.assertEquals(productsPage.getAllProductNames(), beforeToggle,
                "Toggling back did not restore the original product list!");
        softAssert.assertAll();
    }

    @Test(description = "Opening a product shows matching details")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestCaseKey(value = "SAUCEM-103")
    @TestTag(name = "tcId", value = "SL-03")
    @TestTag(name = "feature", value = "Product Grid")
    public void testOpenProductDetailsFromGrid() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        Product fromGrid = productsPage.findRenderedCards().get(ZERO).toProduct();

        ProductDetailsPage detailsPage = productsPage.openProduct(ZERO);
        Assert.assertTrue(detailsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCT_DETAILS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(detailsPage.getProductName(), fromGrid.getName(),
                "Product name on the details page does not match the grid!");
        softAssert.assertEquals(detailsPage.getPriceLabel(), fromGrid.getPriceLabel(),
                "Product price on the details page does not match the grid!");
        softAssert.assertFalse(detailsPage.getProductDescription().trim().isEmpty(),
                "Product description is empty!");
        softAssert.assertTrue(detailsPage.isAddToCartDisplayed(),
                "ADD TO CART button is not displayed on the details page!");
        softAssert.assertAll();
    }

    @Test(description = "Back from product details returns to an unchanged grid")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P2)
    @TestCaseKey(value = "SAUCEM-104")
    @TestTag(name = "tcId", value = "SL-04")
    @TestTag(name = "feature", value = "Product Grid")
    public void testReturnFromDetailsToGrid() {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        List<String> beforeOpening = productsPage.getAllProductNames();

        ProductDetailsPage detailsPage = productsPage.openProduct(ONE);
        Assert.assertTrue(detailsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCT_DETAILS_PAGE_NOT_OPENED);

        ProductsPage returnedPage = detailsPage.goBackToProducts();
        Assert.assertTrue(returnedPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(returnedPage.getAllProductNames(), beforeOpening,
                "The product grid changed after returning from the details page!");
        softAssert.assertEquals(returnedPage.getCartBadgeCount(), ZERO,
                "A cart badge appeared even though nothing was added!");
        softAssert.assertAll();
    }
}
