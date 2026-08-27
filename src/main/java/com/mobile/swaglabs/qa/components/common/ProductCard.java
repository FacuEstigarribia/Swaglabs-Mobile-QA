package com.mobile.swaglabs.qa.components.common;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.IConstants;
import com.mobile.swaglabs.qa.data.Product;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

/**
 * One product tile in the grid.
 */
public class ProductCard extends AbstractUIObject implements IConstants {

    @ExtendedFindBy(accessibilityId = "test-Item title")
    private ExtendedWebElement labelName;

    @ExtendedFindBy(accessibilityId = "test-Price")
    private ExtendedWebElement labelPrice;

    @ExtendedFindBy(accessibilityId = "test-ADD TO CART")
    private ExtendedWebElement buttonAddToCart;

    @ExtendedFindBy(accessibilityId = "test-REMOVE")
    private ExtendedWebElement buttonRemove;

    public ProductCard(WebDriver driver, SearchContext searchContext) {
        super(driver, searchContext);
    }

    /** Named {@code getProductName} because {@code ExtendedWebElement.getName()} is final. */
    public String getProductName() {
        return labelName.getText();
    }

    public String getPriceLabel() {
        return labelPrice.getText();
    }

    public Product toProduct() {
        return new Product(getProductName(), getPriceLabel());
    }

    /**
     * Whether this card is fully rendered.
     *
     * <p>Android virtualizes the grid: a card scrolled halfway into view exposes its title but not
     * its price or buttons. Such a card is skipped when collecting the catalog.
     */
    public boolean isFullyRendered() {
        return labelName.isElementPresent(ONE) && labelPrice.isElementPresent(ONE);
    }

    public boolean isNameDisplayed() {
        return labelName.isElementPresent(ONE);
    }

    public boolean isPriceDisplayed() {
        return labelPrice.isElementPresent(ONE);
    }

    public boolean isAddToCartDisplayed() {
        return buttonAddToCart.isElementPresent(ONE);
    }

    public boolean isRemoveDisplayed() {
        return buttonRemove.isElementPresent(ONE);
    }

    public ExtendedWebElement getAddToCartButton() {
        return buttonAddToCart;
    }

    public ExtendedWebElement getRemoveButton() {
        return buttonRemove;
    }

    public ExtendedWebElement getNameElement() {
        return labelName;
    }
}
