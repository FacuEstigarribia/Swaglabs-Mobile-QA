package com.mobile.swaglabs.qa.pages.ios;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.LoginPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

@DeviceType(pageType = Type.IOS_PHONE, parentClass = LoginPage.class)
public class IOSLoginPage extends LoginPage {

    /** XCUITest surfaces the banner text on the container's accessibility label. */
    private static final String LABEL = "label";

    /**
     * The in-field cross icon is a static text named {@code iconIcon}, in the wrapper that follows
     * the field inside their shared row.
     */
    private static final String FIELD_ERROR_ICON =
            "//%s[@name='%s']/parent::*/following-sibling::XCUIElementTypeOther"
                    + "//XCUIElementTypeStaticText[@name='iconIcon']";

    private static final String USERNAME_TYPE = "XCUIElementTypeTextField";
    private static final String PASSWORD_TYPE = "XCUIElementTypeSecureTextField";

    public IOSLoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void tap(ExtendedWebElement element) {
        IOSTouch.tap(getDriver(), element);
    }

    @Override
    public String getErrorMessageText() {
        return labelErrorMessage.getAttribute(LABEL);
    }

    @Override
    public boolean isUsernameErrorIconDisplayed() {
        return isErrorIconDisplayed(USERNAME_TYPE, "test-Username");
    }

    @Override
    public boolean isPasswordErrorIconDisplayed() {
        return isErrorIconDisplayed(PASSWORD_TYPE, "test-Password");
    }

    private boolean isErrorIconDisplayed(String elementType, String fieldAccessibilityId) {
        String xpath = String.format(FIELD_ERROR_ICON, elementType, fieldAccessibilityId);
        return findExtendedWebElement(By.xpath(xpath), ONE).isElementPresent(ONE);
    }
}
