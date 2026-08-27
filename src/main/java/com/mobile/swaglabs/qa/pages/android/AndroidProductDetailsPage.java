package com.mobile.swaglabs.qa.pages.android;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.pages.common.ProductDetailsPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

import io.appium.java_client.AppiumBy;

@DeviceType(pageType = Type.ANDROID_PHONE, parentClass = ProductDetailsPage.class)
public class AndroidProductDetailsPage extends ProductDetailsPage {

    /** Name then blurb, as the two text nodes of the description block. */
    private static final By DESCRIPTION_TEXTS =
            By.xpath("//*[@content-desc='test-Description']//android.widget.TextView");

    private static final By ITEM_PAGE = AppiumBy.accessibilityId("test-Inventory item page");

    /** Enough gestures to reach the button past a full-height product image. */
    private static final int MAX_REVEAL_GESTURES = 4;

    public AndroidProductDetailsPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected void revealAddToCart() {
        for (int i = ZERO; i < MAX_REVEAL_GESTURES; i++) {
            if (buttonAddToCart.isElementPresent(ONE) || buttonRemove.isElementPresent(ONE)) {
                return;
            }
            if (!AndroidScroll.down(getDriver(), ITEM_PAGE)) {
                return;
            }
        }
    }

    @Override
    public String getProductName() {
        return descriptionText(ZERO);
    }

    @Override
    public String getProductDescription() {
        return descriptionText(ONE);
    }

    private String descriptionText(int index) {
        List<ExtendedWebElement> texts = findExtendedWebElements(DESCRIPTION_TEXTS, DEFAULT_TIMEOUT);
        return texts.size() > index ? texts.get(index).getText() : "";
    }

    @Override
    public int getCartBadgeCount() {
        return AndroidCartBadge.read(getDriver());
    }
}
