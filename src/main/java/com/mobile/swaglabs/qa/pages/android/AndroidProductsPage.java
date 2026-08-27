package com.mobile.swaglabs.qa.pages.android;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.enums.SortOption;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

import io.appium.java_client.AppiumBy;

@DeviceType(pageType = Type.ANDROID_PHONE, parentClass = ProductsPage.class)
public class AndroidProductsPage extends ProductsPage {

    private static final String SORT_OPTION = "//android.widget.TextView[@text='%s']";

    private static final By PRODUCTS_CONTAINER = AppiumBy.accessibilityId("test-PRODUCTS");

    public AndroidProductsPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Android virtualizes the grid, keeping only cards near the viewport in the hierarchy, so the
     * catalog has to be scrolled through to be read in full.
     */
    @Override
    protected boolean scrollForMoreProducts() {
        // A part-page step: grid rows are tall enough that a full page can land with a card's
        // price and buttons still unrendered, and that card is then skipped.
        return AndroidScroll.down(getDriver(), PRODUCTS_CONTAINER, AndroidScroll.PART_PAGE);
    }

    @Override
    protected void resetScrollPosition() {
        AndroidScroll.toTop(getDriver(), PRODUCTS_CONTAINER);
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
        return AndroidCartBadge.read(getDriver());
    }
}
