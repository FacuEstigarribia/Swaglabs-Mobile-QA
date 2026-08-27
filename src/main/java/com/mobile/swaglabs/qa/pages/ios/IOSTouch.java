package com.mobile.swaglabs.qa.pages.ios;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

/**
 * Taps iOS elements, correcting for the app being letterboxed.
 *
 * <p>The Swag Labs build ships no launch storyboard for current iPhone screens, so iOS renders it
 * scaled and centred. WebDriverAgent reports element rectangles in that inset app space (390x844)
 * while touches are delivered in screen space (402x874), which puts every tap about 15pt too high.
 *
 * <p>Large controls absorb the error, but the 50pt header buttons do not: a plain
 * {@code click()} on the cart or the hamburger menu silently does nothing. Confirmed by a tap
 * sweep (the cart opens at y=60, not at its reported centre of y=50) and by
 * {@code mobile: deviceScreenInfo}, which reports the 402x874 screen behind the 390x844 window.
 *
 * <p>The inset is measured per session rather than hard-coded, so it recomputes for a different
 * simulator and collapses to zero on a device the app supports natively.
 */
final class IOSTouch {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SCREEN_INFO_SCRIPT = "mobile: deviceScreenInfo";
    private static final Duration PRESS_DURATION = Duration.ofMillis(120);

    private IOSTouch() {
    }

    /** Taps the centre of {@code element}, offset by the letterbox inset. */
    static void tap(WebDriver driver, ExtendedWebElement element) {
        Rectangle rect = element.getRect();
        Point inset = letterboxInset(driver);
        int x = rect.getX() + rect.getWidth() / 2 + inset.getX();
        int y = rect.getY() + rect.getHeight() / 2 + inset.getY();
        LOGGER.debug("Tapping at ({}, {}) using letterbox inset ({}, {}).", x, y, inset.getX(), inset.getY());
        tapAt(driver, x, y);
    }

    private static void tapAt(WebDriver driver, int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, PRESS_DURATION))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        // Cast to Interactive, not RemoteWebDriver: Carina hands back a decorated driver that is
        // not a RemoteWebDriver, so that cast would throw at runtime.
        ((Interactive) driver).perform(Collections.singletonList(tap));
    }

    /**
     * Half the difference between the physical screen and the app window, which is how far the
     * letterboxed app is inset from the screen origin.
     *
     * <p>Falls back to no offset if the screen cannot be queried, which degrades to plain
     * behaviour rather than throwing.
     */
    private static Point letterboxInset(WebDriver driver) {
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(SCREEN_INFO_SCRIPT);
            if (!(raw instanceof Map)) {
                return new Point(0, 0);
            }
            Object screen = ((Map<?, ?>) raw).get("screenSize");
            if (!(screen instanceof Map)) {
                return new Point(0, 0);
            }
            Map<?, ?> size = (Map<?, ?>) screen;
            int screenWidth = ((Number) size.get("width")).intValue();
            int screenHeight = ((Number) size.get("height")).intValue();

            Dimension window = driver.manage().window().getSize();
            return new Point((screenWidth - window.getWidth()) / 2, (screenHeight - window.getHeight()) / 2);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not determine the letterbox inset; tapping without an offset.", e);
            return new Point(0, 0);
        }
    }
}
