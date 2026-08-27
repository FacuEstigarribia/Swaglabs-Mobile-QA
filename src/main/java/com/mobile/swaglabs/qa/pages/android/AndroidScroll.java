package com.mobile.swaglabs.qa.pages.android;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.remote.RemoteWebElement;

/**
 * Scrolls a scrollable container on Android.
 *
 * <p>Uses UiAutomator2's {@code mobile: scrollGesture} rather than a swipe. A plain swipe is not
 * usable in this app: the product grid supports drag-to-cart, so a slow drag beginning on a card is
 * interpreted as picking the card up instead of scrolling. The scroll gesture acts on the container
 * directly and cannot be mistaken for a drag.
 *
 * <p>A static helper rather than a shared base class: Carina's {@code @DeviceType} resolution
 * requires each platform page to extend the common abstract page, leaving no room for a
 * per-platform superclass.
 */
final class AndroidScroll {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(AndroidScroll.class);

    private static final String SCROLL_GESTURE = "mobile: scrollGesture";
    private static final String DOWN = "down";
    private static final String UP = "up";

    /**
     * A full page per gesture. Fine for the product grid, whose rows are short enough that a full
     * page still overlaps by a row.
     */
    static final double FULL_PAGE = 1.0;

    /**
     * A part-page gesture, for lists with tall rows.
     *
     * <p>Cart rows are roughly a third of the screen each, and a full-page scroll can land with a
     * row's name above the viewport and only its description showing — which reads as a row that
     * is present but unidentifiable.
     */
    static final double PART_PAGE = 0.5;

    /** Bounds the rewind loop so a misbehaving container cannot spin forever. */
    private static final int MAX_REWIND_GESTURES = 10;

    private AndroidScroll() {
    }

    /**
     * Scrolls {@code container} down by one gesture.
     *
     * @return {@code true} while there is more content below
     */
    static boolean down(WebDriver driver, By container) {
        return down(driver, container, FULL_PAGE);
    }

    static boolean down(WebDriver driver, By container, double percent) {
        return scroll(driver, container, DOWN, percent);
    }

    /** Scrolls {@code container} back to the top, so a later pass starts from a known position. */
    static void toTop(WebDriver driver, By container) {
        for (int i = 0; i < MAX_REWIND_GESTURES; i++) {
            if (!scroll(driver, container, UP, FULL_PAGE)) {
                return;
            }
        }
    }

    private static boolean scroll(WebDriver driver, By container, String direction, double percent) {
        try {
            return performScroll(driver, container, direction, percent);
        } catch (StaleElementReferenceException e) {
            // Actions that re-render the list — toggling between grid and list view, for one —
            // can invalidate the container between locating it and the gesture reaching the
            // device. Locating it again is enough; a second failure is a real problem.
            LOGGER.debug("Scroll container went stale mid-gesture; retrying with a fresh lookup.");
            return performScroll(driver, container, direction, percent);
        }
    }

    private static boolean performScroll(WebDriver driver, By container, String direction, double percent) {
        Map<String, Object> args = new HashMap<>();
        // Addressed by element rather than by bounding box: given a box UiAutomator2 reports
        // nothing to scroll, whereas the element form scrolls the container correctly.
        args.put("elementId", remoteIdOf(driver.findElement(container)));
        args.put("direction", direction);
        args.put("percent", percent);
        Object canScrollMore = ((JavascriptExecutor) driver).executeScript(SCROLL_GESTURE, args);
        return Boolean.TRUE.equals(canScrollMore);
    }

    /**
     * The remote id the Appium server knows an element by.
     *
     * <p>Carina wraps every element in listener proxies, so what comes back is never a
     * {@link RemoteWebElement} directly and has to be unwrapped before its id can be read.
     */
    private static String remoteIdOf(WebElement element) {
        WebElement current = element;
        while (current instanceof WrapsElement) {
            current = ((WrapsElement) current).getWrappedElement();
        }
        return ((RemoteWebElement) current).getId();
    }
}
