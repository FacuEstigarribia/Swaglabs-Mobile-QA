package com.mobile.swaglabs.qa.components.common;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.IConstants;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractUIObject;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

/**
 * One line item on the cart screen.
 */
public class CartItem extends AbstractUIObject implements IConstants {

    @ExtendedFindBy(accessibilityId = "test-Description")
    private ExtendedWebElement description;

    @ExtendedFindBy(accessibilityId = "test-Price")
    private ExtendedWebElement price;

    @ExtendedFindBy(accessibilityId = "test-Amount")
    private ExtendedWebElement amount;

    @ExtendedFindBy(accessibilityId = "test-REMOVE")
    private ExtendedWebElement buttonRemove;

    public CartItem(WebDriver driver, SearchContext searchContext) {
        super(driver, searchContext);
    }

    public ExtendedWebElement getDescription() {
        return description;
    }

    public ExtendedWebElement getPrice() {
        return price;
    }

    public ExtendedWebElement getAmount() {
        return amount;
    }

    public ExtendedWebElement getRemoveButton() {
        return buttonRemove;
    }
}
