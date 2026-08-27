package com.mobile.swaglabs.qa.pages.ios;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumBy;

/**
 * Reads the iOS cart badge.
 *
 * <p>iOS publishes the count as the accessibility label of the cart button itself, whereas Android
 * nests it in a child text node — hence the platform-specific reader.
 */
final class IOSCartBadge {

    private static final By CART = AppiumBy.accessibilityId("test-Cart");
    private static final String LABEL = "label";
    private static final String DIGITS = "\\d+";

    private IOSCartBadge() {
    }

    /**
     * The badge count, or 0 when the cart button carries no count.
     *
     * <p>Retried once: adding to or removing from the cart re-renders the button, so it can go
     * stale between being located and being read.
     */
    static int read(WebDriver driver) {
        try {
            return readOnce(driver);
        } catch (StaleElementReferenceException e) {
            return readOnce(driver);
        }
    }

    private static int readOnce(WebDriver driver) {
        List<WebElement> carts = driver.findElements(CART);
        for (WebElement cart : carts) {
            String label = cart.getAttribute(LABEL);
            if (label != null && label.trim().matches(DIGITS)) {
                return Integer.parseInt(label.trim());
            }
        }
        return 0;
    }
}
