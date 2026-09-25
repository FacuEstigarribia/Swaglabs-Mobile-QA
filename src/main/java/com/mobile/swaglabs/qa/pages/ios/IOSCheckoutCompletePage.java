package com.mobile.swaglabs.qa.pages.ios;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.CheckoutCompletePage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

@DeviceType(pageType = Type.IOS_PHONE, parentClass = CheckoutCompletePage.class)
public class IOSCheckoutCompletePage extends CheckoutCompletePage {

    public IOSCheckoutCompletePage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void tap(ExtendedWebElement element) {
        IOSTouch.tap(getDriver(), element);
    }
}
