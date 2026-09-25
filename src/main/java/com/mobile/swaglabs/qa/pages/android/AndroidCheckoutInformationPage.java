package com.mobile.swaglabs.qa.pages.android;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.CheckoutInformationPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;

@DeviceType(pageType = Type.ANDROID_PHONE, parentClass = CheckoutInformationPage.class)
public class AndroidCheckoutInformationPage extends CheckoutInformationPage {

    private static final String ERROR_TEXT =
            "//*[@content-desc='test-Error message']//android.widget.TextView";

    public AndroidCheckoutInformationPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getErrorMessageText() {
        return findExtendedWebElement(By.xpath(ERROR_TEXT), DEFAULT_TIMEOUT).getText();
    }
}
