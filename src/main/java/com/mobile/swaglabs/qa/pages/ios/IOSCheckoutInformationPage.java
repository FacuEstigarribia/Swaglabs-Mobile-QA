package com.mobile.swaglabs.qa.pages.ios;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.CheckoutInformationPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

@DeviceType(pageType = Type.IOS_PHONE, parentClass = CheckoutInformationPage.class)
public class IOSCheckoutInformationPage extends CheckoutInformationPage {

    public IOSCheckoutInformationPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void tap(ExtendedWebElement element) {
        IOSTouch.tap(getDriver(), element);
    }

    @Override
    public String getErrorMessageText() {
        return labelErrorMessage.getAttribute("label");
    }
}
