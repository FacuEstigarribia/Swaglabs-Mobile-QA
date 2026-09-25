package com.mobile.swaglabs.qa.service;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;

import com.mobile.swaglabs.qa.IConstants;
import com.mobile.swaglabs.qa.pages.common.CartPage;
import com.mobile.swaglabs.qa.pages.common.CheckoutCompletePage;
import com.mobile.swaglabs.qa.pages.common.CheckoutInformationPage;
import com.mobile.swaglabs.qa.pages.common.CheckoutOverviewPage;
import com.zebrunner.carina.utils.factory.ICustomTypePageFactory;
import com.zebrunner.carina.webdriver.IDriverPool;

public class CheckoutService implements IDriverPool, ICustomTypePageFactory, IConstants {

    private final WebDriver driver;

    public CheckoutService(WebDriver driver) {
        this.driver = driver;
    }

    public CheckoutInformationPage startCheckout() {
        CartPage cartPage = initPage(driver, CartPage.class);
        Assert.assertTrue(cartPage.isPageOpened(DEFAULT_TIMEOUT), CART_PAGE_NOT_OPENED);
        CheckoutInformationPage informationPage = cartPage.checkout();
        Assert.assertTrue(informationPage.isPageOpened(DEFAULT_TIMEOUT),
                CHECKOUT_INFORMATION_PAGE_NOT_OPENED);
        return informationPage;
    }

    public CheckoutOverviewPage submitInformation(CheckoutInformationPage informationPage,
            String firstName, String lastName, String postalCode) {
        informationPage.fillInformation(firstName, lastName, postalCode);
        CheckoutOverviewPage overviewPage = informationPage.submit();
        Assert.assertTrue(overviewPage.isPageOpened(DEFAULT_TIMEOUT),
                CHECKOUT_OVERVIEW_PAGE_NOT_OPENED);
        return overviewPage;
    }

    public CheckoutCompletePage finishCheckout(CheckoutOverviewPage overviewPage) {
        CheckoutCompletePage completePage = overviewPage.finish();
        Assert.assertTrue(completePage.isPageOpened(DEFAULT_TIMEOUT),
                CHECKOUT_COMPLETE_PAGE_NOT_OPENED);
        return completePage;
    }
}
