package com.mobile.swaglabs.qa.pages.common;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.data.Product;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

/** The detail screen for a single product. */
public abstract class ProductDetailsPage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-Inventory item page")
    protected ExtendedWebElement itemPageContainer;

    @ExtendedFindBy(accessibilityId = "test-BACK TO PRODUCTS")
    protected ExtendedWebElement buttonBackToProducts;

    @ExtendedFindBy(accessibilityId = "test-Description")
    protected ExtendedWebElement description;

    @ExtendedFindBy(accessibilityId = "test-Price")
    protected ExtendedWebElement labelPrice;

    @ExtendedFindBy(accessibilityId = "test-ADD TO CART")
    protected ExtendedWebElement buttonAddToCart;

    @ExtendedFindBy(accessibilityId = "test-REMOVE")
    protected ExtendedWebElement buttonRemove;

    @ExtendedFindBy(accessibilityId = "test-Cart")
    protected ExtendedWebElement buttonCart;

    protected ProductDetailsPage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(itemPageContainer);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    /**
     * Brings ADD TO CART / REMOVE into view.
     */
    protected abstract void revealAddToCart();

    /** The product name, which is the first text node of the description block. */
    public abstract String getProductName();

    /** The product blurb, which is the second text node of the description block. */
    public abstract String getProductDescription();

    public abstract int getCartBadgeCount();

    public String getPriceLabel() {
        return labelPrice.getText();
    }

    public Product toProduct() {
        return new Product(getProductName(), getPriceLabel());
    }

    public boolean isAddToCartDisplayed() {
        revealAddToCart();
        return buttonAddToCart.isElementPresent(DEFAULT_TIMEOUT);
    }

    public boolean isRemoveDisplayed() {
        revealAddToCart();
        return buttonRemove.isElementPresent(DEFAULT_TIMEOUT);
    }

    public void addToCart() {
        LOGGER.info("Adding the displayed product to the cart.");
        revealAddToCart();
        tap(buttonAddToCart);
    }

    public ProductsPage goBackToProducts() {
        LOGGER.info("Returning to the product grid.");
        tap(buttonBackToProducts);
        return initPage(getDriver(), ProductsPage.class);
    }

    public CartPage openCart() {
        LOGGER.info("Opening the cart from the product details page.");
        tap(buttonCart);
        return initPage(getDriver(), CartPage.class);
    }
}
