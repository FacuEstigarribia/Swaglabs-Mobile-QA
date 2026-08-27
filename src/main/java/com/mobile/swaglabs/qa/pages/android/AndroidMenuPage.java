package com.mobile.swaglabs.qa.pages.android;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.MenuPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;

@DeviceType(pageType = Type.ANDROID_PHONE, parentClass = MenuPage.class)
public class AndroidMenuPage extends MenuPage {

    public AndroidMenuPage(WebDriver driver) {
        super(driver);
    }
}
