package com.mobile.swaglabs.qa.pages.ios;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.remote.RemoteWebElement;

/**
 * Scrolls a scrollable container on iOS.
 *
 * <p>Uses XCUITest's {@code mobile: scroll}. Unlike the Android equivalent it reports nothing about
 * whether more content remains, so callers detect the end of a list by watching for the contents to
 * stop changing.
 */
final class IOSScroll {

    private static final String SCROLL = "mobile: scroll";
    private static final String DOWN = "down";
    private static final String UP = "up";

    private IOSScroll() {
    }

    static void down(WebDriver driver, By container) {
        scroll(driver, container, DOWN);
    }

    static void up(WebDriver driver, By container) {
        scroll(driver, container, UP);
    }

    private static void scroll(WebDriver driver, By container, String direction) {
        Map<String, Object> args = new HashMap<>();
        args.put("elementId", remoteIdOf(driver.findElement(container)));
        args.put("direction", direction);
        ((JavascriptExecutor) driver).executeScript(SCROLL, args);
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
