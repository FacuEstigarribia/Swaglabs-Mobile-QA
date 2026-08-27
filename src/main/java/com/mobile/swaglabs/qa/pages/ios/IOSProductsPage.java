package com.mobile.swaglabs.qa.pages.ios;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.components.common.ProductCard;
import com.mobile.swaglabs.qa.enums.SortOption;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

import io.appium.java_client.AppiumBy;

@DeviceType(pageType = Type.IOS_PHONE, parentClass = ProductsPage.class)
public class IOSProductsPage extends ProductsPage {

    /** Sort options are plain containers named after their label. */
    private static final String SORT_OPTION = "//XCUIElementTypeOther[@name='%s']";

    private static final By PRODUCTS_CONTAINER = AppiumBy.accessibilityId("test-PRODUCTS");

    /** Bounds the rewind loop so a misbehaving container cannot spin forever. */
    private static final int MAX_REWIND_GESTURES = 10;

    public IOSProductsPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void tap(ExtendedWebElement element) {
        IOSTouch.tap(getDriver(), element);
    }

    /**
     * Scrolls the grid down by one screen.
     *
     * <p>iOS virtualizes the list much as Android does — only cards near the viewport carry their
     * price and buttons — so the catalog has to be scrolled through to be read in full.
     *
     * <p>{@code mobile: scroll} reports nothing about remaining content, so the end of the list is
     * detected by the topmost card no longer changing.
     */
    @Override
    protected boolean scrollForMoreProducts() {
        String before = renderedSignature();
        IOSScroll.down(getDriver(), PRODUCTS_CONTAINER);
        return !renderedSignature().equals(before);
    }

    @Override
    protected void resetScrollPosition() {
        for (int i = ZERO; i < MAX_REWIND_GESTURES; i++) {
            String before = renderedSignature();
            IOSScroll.up(getDriver(), PRODUCTS_CONTAINER);
            if (renderedSignature().equals(before)) {
                return;
            }
        }
    }

    /**
     * A "did the list move" signal covering every rendered card.
     *
     * <p>Deliberately not just the topmost card: a list can scroll far enough to bring a new card
     * into view while the first stays put, and a top-only signal reads that as "nothing moved".
     */
    private String renderedSignature() {
        StringBuilder signature = new StringBuilder();
        for (ProductCard card : findRenderedCards()) {
            signature.append(card.getProductName()).append('|');
        }
        return signature.toString();
    }

    @Override
    public void sortBy(SortOption option) {
        LOGGER.info("Sorting products by '{}'.", option.getLabel());
        tap(buttonSortSelector);
        ExtendedWebElement optionElement =
                findExtendedWebElement(By.xpath(String.format(SORT_OPTION, option.getLabel())), DEFAULT_TIMEOUT);
        tap(optionElement);
    }

    @Override
    public int getCartBadgeCount() {
        return IOSCartBadge.read(getDriver());
    }
}
