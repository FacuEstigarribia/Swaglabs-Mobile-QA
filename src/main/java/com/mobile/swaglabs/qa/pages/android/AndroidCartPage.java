package com.mobile.swaglabs.qa.pages.android;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.components.common.CartItem;
import com.mobile.swaglabs.qa.pages.common.CartPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

import io.appium.java_client.AppiumBy;

@DeviceType(pageType = Type.ANDROID_PHONE, parentClass = CartPage.class)
public class AndroidCartPage extends CartPage {

    private static final By CHILD_TEXTS = By.xpath(".//android.widget.TextView");

    private static final By CART_CONTENT = AppiumBy.accessibilityId("test-Cart Content");

    /** Enough gestures to reach the bottom of a cart holding the whole catalog. */
    private static final int MAX_REVEAL_GESTURES = 6;

    public AndroidCartPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Android drops off-screen content from the view hierarchy, so a cart with more items than fit
     * on screen has to be scrolled through to be read in full.
     */
    @Override
    protected boolean scrollForMoreItems() {
        return AndroidScroll.down(getDriver(), CART_CONTENT, AndroidScroll.PART_PAGE);
    }

    @Override
    protected void resetScrollPosition() {
        AndroidScroll.toTop(getDriver(), CART_CONTENT);
    }

    /**
     * Scrolls the cart until the action buttons are on screen.
     */
    @Override
    protected void revealActions() {
        for (int i = ZERO; i < MAX_REVEAL_GESTURES; i++) {
            if (buttonContinueShopping.isElementPresent(ONE)) {
                return;
            }
            if (!AndroidScroll.down(getDriver(), CART_CONTENT, AndroidScroll.PART_PAGE)) {
                return;
            }
        }
    }

    /**
     * The row's product name.
     */
    @Override
    protected String readItemName(CartItem item) {
        List<ExtendedWebElement> texts = textsIn(item.getDescription());
        if (texts.size() < TWO) {
            return "";
        }
        String text = texts.get(ZERO).getText();
        return text == null ? "" : text.trim();
    }

    @Override
    protected String readItemPrice(CartItem item) {
        return firstText(item.getPrice());
    }

    /**
     * The first text under {@code container}, or "" when the container is not there.
     */
    private String firstText(ExtendedWebElement container) {
        List<ExtendedWebElement> texts = textsIn(container);
        String text = texts.isEmpty() ? container.getText() : texts.get(ZERO).getText();
        return text == null ? "" : text.trim();
    }

    /** Text nodes under {@code container}, empty when the container itself is not rendered. */
    private List<ExtendedWebElement> textsIn(ExtendedWebElement container) {
        if (!container.isElementPresent(DEFAULT_TIMEOUT)) {
            return Collections.emptyList();
        }
        return findExtendedWebElements(container, CHILD_TEXTS, Duration.ofSeconds(ONE));
    }

    @Override
    public int getCartBadgeCount() {
        return AndroidCartBadge.read(getDriver());
    }
}
