package com.mobile.swaglabs.qa.pages.common;

import org.openqa.selenium.WebDriver;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

public abstract class CheckoutCompletePage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-CHECKOUT: COMPLETE!")
    protected ExtendedWebElement completeContent;

    @ExtendedFindBy(accessibilityId = "test-BACK HOME")
    protected ExtendedWebElement buttonBackHome;

    protected CheckoutCompletePage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(completeContent);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    public boolean isBackHomeDisplayed() {
        return buttonBackHome.isElementPresent(DEFAULT_TIMEOUT);
    }

    public ProductsPage backHome() {
        tap(buttonBackHome);
        return initPage(getDriver(), ProductsPage.class);
    }
}
