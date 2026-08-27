package com.mobile.swaglabs.qa.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

/**
 * Colour assertions for native screens.
 *
 * <p>UiAutomator2 and XCUITest expose no colour or style attribute, so a border colour cannot be
 * read the way a web test would read {@code border-color}.
 * <p>Validated against the live app: the clean Swag Labs login screen yields 0% red in that band
 * and the rejected-credentials state yields 100%, so the check discriminates rather than always
 * passing.
 */
public final class ColorUtil {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final Color ERROR_RED = new Color(226, 35, 26);

    /** Per-channel tolerance, wide enough for antialiasing and the two platforms' rendering. */
    private static final int CHANNEL_TOLERANCE = 60;

    /**
     * How far below the element to look, in driver units. The app draws an 8pt underline flush
     * with the bottom edge; the extra room absorbs the sub-pixel error in the image-space mapping.
     */
    private static final int BAND_DEPTH = 14;

    /** Ignore the outer tenth of the width, where rounded corners fade the colour out. */
    private static final double HORIZONTAL_INSET = 0.1;

    /** Fraction of sampled pixels that must be red for a scanline to count as a border. */
    private static final double REQUIRED_RED_FRACTION = 0.9;

    private static final int SAMPLES_PER_LINE = 20;

    private ColorUtil() {
    }

    /**
     * Whether a red border is drawn immediately below {@code element}.
     *
     * <p>Returns {@code false} rather than throwing if the screenshot cannot be read, so a
     * screenshot problem surfaces as a failed assertion with a logged cause instead of an error
     * that masks the real result.
     */
    public static boolean hasRedBorderBelow(WebDriver driver, ExtendedWebElement element) {
        try {
            BufferedImage image = takeScreenshot(driver);
            double scale = resolveScale(driver, image);
            Rectangle rect = element.getRect();

            int left = (int) Math.round((rect.getX() + rect.getWidth() * HORIZONTAL_INSET) * scale);
            int right = (int) Math.round((rect.getX() + rect.getWidth() * (1 - HORIZONTAL_INSET)) * scale);
            int top = (int) Math.round((rect.getY() + rect.getHeight()) * scale);
            int bottom = (int) Math.round((rect.getY() + rect.getHeight() + BAND_DEPTH) * scale);

            for (int y = top; y <= bottom; y++) {
                if (isRedScanline(image, left, right, y)) {
                    return true;
                }
            }
            LOGGER.info("No red border found below the element in the band y={}..{} (image {}x{}, scale {}).",
                    top, bottom, image.getWidth(), image.getHeight(), scale);
            return false;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not evaluate the border colour; treating it as not red.", e);
            return false;
        }
    }

    private static boolean isRedScanline(BufferedImage image, int left, int right, int y) {
        if (y < 0 || y >= image.getHeight()) {
            return false;
        }
        int step = Math.max(1, (right - left) / SAMPLES_PER_LINE);
        int sampled = 0;
        int red = 0;
        for (int x = left; x <= right && x < image.getWidth(); x += step) {
            if (x < 0) {
                continue;
            }
            sampled++;
            if (matches(new Color(image.getRGB(x, y)), ERROR_RED)) {
                red++;
            }
        }
        return sampled > 0 && (double) red / sampled >= REQUIRED_RED_FRACTION;
    }

    private static boolean matches(Color actual, Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= CHANNEL_TOLERANCE
                && Math.abs(actual.getGreen() - expected.getGreen()) <= CHANNEL_TOLERANCE
                && Math.abs(actual.getBlue() - expected.getBlue()) <= CHANNEL_TOLERANCE;
    }

    private static BufferedImage takeScreenshot(WebDriver driver) throws IOException {
        byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /**
     * Pixels per driver unit.
     *
     * <p>Derived from width alone, deliberately. Pixels are square, so one factor covers both axes,
     * whereas a height-derived factor would be skewed by status and navigation bars that appear in
     * the screenshot but not in the window rectangle.
     */
    private static double resolveScale(WebDriver driver, BufferedImage image) {
        Dimension window = driver.manage().window().getSize();
        return (double) image.getWidth() / window.getWidth();
    }
}
