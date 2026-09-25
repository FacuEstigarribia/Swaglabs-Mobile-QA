package com.mobile.swaglabs.qa.pages.android;

import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.CheckoutCompletePage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;

@DeviceType(pageType = Type.ANDROID_PHONE, parentClass = CheckoutCompletePage.class)
public class AndroidCheckoutCompletePage extends CheckoutCompletePage {

    public AndroidCheckoutCompletePage(WebDriver driver) {
        super(driver);
    }
}
