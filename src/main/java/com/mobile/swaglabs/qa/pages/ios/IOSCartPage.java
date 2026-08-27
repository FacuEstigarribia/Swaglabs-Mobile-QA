package com.mobile.swaglabs.qa.pages.ios;

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

@DeviceType(pageType = Type.IOS_PHONE, parentClass = CartPage.class)
public class IOSCartPage extends CartPage {

    private static final By CHILD_TEXTS = By.xpath(".//XCUIElementTypeStaticText");

    private static final By CART_CONTENT = AppiumBy.accessibilityId("test-Cart Content");

    /** Bounds the rewind loop so a misbehaving container cannot spin forever. */
    private static final int MAX_REWIND_GESTURES = 10;

    public IOSCartPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void tap(ExtendedWebElement element) {
        IOSTouch.tap(getDriver(), element);
    }

    /**
     * Scrolls the cart down by one screen.
     *
     * <p>{@code mobile: scroll} reports nothing about remaining content, so the end of the list is
     * detected by the topmost line item no longer changing.
     */
    @Override
    protected boolean scrollForMoreItems() {
        String before = renderedSignature();
        IOSScroll.down(getDriver(), CART_CONTENT);
        return !renderedSignature().equals(before);
    }

    @Override
    protected void resetScrollPosition() {
        for (int i = ZERO; i < MAX_REWIND_GESTURES; i++) {
            String before = renderedSignature();
            IOSScroll.up(getDriver(), CART_CONTENT);
            if (renderedSignature().equals(before)) {
                return;
            }
        }
    }

    /**
     * A "did the list move" signal covering every rendered row.
     *
     * <p>Deliberately not just the topmost row: a short list can scroll far enough to bring a new
     * row into view while the first row stays put, and a top-only signal reads that as "nothing
     * moved" and stops collecting one row early.
     */
    private String renderedSignature() {
        StringBuilder signature = new StringBuilder();
        for (CartItem item : findRenderedItems()) {
            signature.append(readItemName(item)).append('|');
        }
        return signature.toString();
    }

    /** The buttons sit below the line items, so a longer cart pushes them off screen. */
    @Override
    protected void revealActions() {
        for (int i = ZERO; i < MAX_REWIND_GESTURES; i++) {
            if (buttonContinueShopping.isElementPresent(ONE)) {
                return;
            }
            String before = renderedSignature();
            IOSScroll.down(getDriver(), CART_CONTENT);
            if (renderedSignature().equals(before)) {
                return;
            }
        }
    }

    /**
     * The row's product name.
     *
     * <p>A fully rendered description block holds two text nodes: the name, then the long blurb.
     * A row that is only partially rendered exposes just one — and it may be the blurb, so taking
     * "the first text node" unconditionally yields the description where the name was expected.
     * Requiring both nodes distinguishes "not ready yet" from "ready", and returning "" lets the
     * badge-checked read in {@link #getItems()} retry.
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
     *
     * <p>Tolerant by design. {@code test-Item} is also the id of a *product grid* card, so a
     * collection pass that catches the screen mid-transition can match a card that has no
     * description block. Returning "" lets the caller skip it; throwing would fail the test for a
     * transient element that is not a cart line at all.
     *
     * <p>The wait is the full default rather than a token second: a genuine cart row that is still
     * rendering would otherwise be indistinguishable from a product card, and get silently dropped
     * from the collection — which surfaces as a cart that is short by one item.
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
        return IOSCartBadge.read(getDriver());
    }
}
