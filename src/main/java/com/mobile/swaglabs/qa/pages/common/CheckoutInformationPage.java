package com.mobile.swaglabs.qa.pages.common;

import org.openqa.selenium.WebDriver;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

public abstract class CheckoutInformationPage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-First Name")
    protected ExtendedWebElement fieldFirstName;

    @ExtendedFindBy(accessibilityId = "test-Last Name")
    protected ExtendedWebElement fieldLastName;

    @ExtendedFindBy(accessibilityId = "test-Zip/Postal Code")
    protected ExtendedWebElement fieldPostalCode;

    @ExtendedFindBy(accessibilityId = "test-CANCEL")
    protected ExtendedWebElement buttonCancel;

    @ExtendedFindBy(accessibilityId = "test-CONTINUE")
    protected ExtendedWebElement buttonContinue;

    @ExtendedFindBy(accessibilityId = "test-Error message")
    protected ExtendedWebElement labelErrorMessage;

    protected CheckoutInformationPage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(fieldFirstName);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    public void fillInformation(String firstName, String lastName, String postalCode) {
        type(fieldFirstName, firstName);
        type(fieldLastName, lastName);
        type(fieldPostalCode, postalCode);
    }

    private void type(ExtendedWebElement field, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        tap(field);
        field.type(value);
    }

    public CheckoutOverviewPage submit() {
        hideKeyboard();
        tap(buttonContinue);
        return initPage(getDriver(), CheckoutOverviewPage.class);
    }

    public ProductsPage cancel() {
        hideKeyboard();
        tap(buttonCancel);
        return initPage(getDriver(), ProductsPage.class);
    }

    public boolean isErrorMessageDisplayed(long timeout) {
        return labelErrorMessage.isElementPresent(timeout);
    }

    public abstract String getErrorMessageText();
}
