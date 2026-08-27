package com.mobile.swaglabs.qa.pages.android;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.LoginPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

@DeviceType(pageType = Type.ANDROID_PHONE, parentClass = LoginPage.class)
public class AndroidLoginPage extends LoginPage {

    /** The banner's message lives in a child text node, not on the banner element itself. */
    private static final String ERROR_TEXT = "//*[@content-desc='test-Error message']//android.widget.TextView";

    /**
     * The in-field cross icon sits in the wrapper that follows the field, inside their shared row.
     */
    private static final String FIELD_ERROR_ICON =
            "//android.widget.EditText[@content-desc='%s']/following-sibling::*//android.widget.TextView";

    public AndroidLoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getErrorMessageText() {
        ExtendedWebElement text = findExtendedWebElement(By.xpath(ERROR_TEXT), DEFAULT_TIMEOUT);
        return text.getText();
    }

    @Override
    public boolean isUsernameErrorIconDisplayed() {
        return isErrorIconDisplayed("test-Username");
    }

    @Override
    public boolean isPasswordErrorIconDisplayed() {
        return isErrorIconDisplayed("test-Password");
    }

    private boolean isErrorIconDisplayed(String fieldAccessibilityId) {
        return findExtendedWebElement(By.xpath(String.format(FIELD_ERROR_ICON, fieldAccessibilityId)), ONE)
                .isElementPresent(ONE);
    }
}
