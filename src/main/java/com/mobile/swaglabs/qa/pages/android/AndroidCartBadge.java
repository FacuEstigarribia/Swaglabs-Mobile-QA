package com.mobile.swaglabs.qa.pages.android;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Reads the Android cart badge.
 *
 * <p>On Android the count is a text node nested inside the cart button, and the node is absent
 * entirely when the cart is empty. (iOS instead publishes the count as the cart button's own
 * accessibility label, which is why this is platform-specific.)
 *
 * <p>A static helper rather than a shared base class: Carina's {@code @DeviceType} resolution
 * requires each platform page to extend the common abstract page, which leaves no room for a
 * per-platform superclass.
 */
final class AndroidCartBadge {

    private static final By BADGE = By.xpath("//*[@content-desc='test-Cart']//android.widget.TextView");
    private static final String DIGITS = "\\d+";

    private AndroidCartBadge() {
    }

    /**
     * The badge count, or 0 when no badge is rendered.
     *
     * <p>Retried once: adding to or removing from the cart re-renders the badge, so it can go stale
     * between being located and being read.
     */
    static int read(WebDriver driver) {
        try {
            return readOnce(driver);
        } catch (StaleElementReferenceException e) {
            return readOnce(driver);
        }
    }

    private static int readOnce(WebDriver driver) {
        List<WebElement> candidates = driver.findElements(BADGE);
        for (WebElement candidate : candidates) {
            String text = candidate.getText();
            if (text != null && text.trim().matches(DIGITS)) {
                return Integer.parseInt(text.trim());
            }
        }
        return 0;
    }
}
