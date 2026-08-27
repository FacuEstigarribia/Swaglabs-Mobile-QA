package com.mobile.swaglabs.qa.pages.ios;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.MenuPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

@DeviceType(pageType = Type.IOS_PHONE, parentClass = MenuPage.class)
public class IOSMenuPage extends MenuPage {

    public IOSMenuPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void tap(ExtendedWebElement element) {
        IOSTouch.tap(getDriver(), element);
    }
}
