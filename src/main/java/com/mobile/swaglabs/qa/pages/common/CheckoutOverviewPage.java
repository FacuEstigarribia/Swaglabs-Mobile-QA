package com.mobile.swaglabs.qa.pages.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.mobile.swaglabs.qa.components.common.CartItem;
import com.mobile.swaglabs.qa.data.Product;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

import io.appium.java_client.AppiumBy;

public abstract class CheckoutOverviewPage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-CHECKOUT: OVERVIEW")
    protected ExtendedWebElement overviewContent;

    @ExtendedFindBy(accessibilityId = "test-CANCEL")
    protected ExtendedWebElement buttonCancel;

    @ExtendedFindBy(accessibilityId = "test-FINISH")
    protected ExtendedWebElement buttonFinish;

    private static final By ITEM = AppiumBy.accessibilityId("test-Item");

    protected CheckoutOverviewPage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(overviewContent);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    protected List<CartItem> findRenderedItems() {
        List<CartItem> items = new ArrayList<>();
        for (WebElement element : getDriver().findElements(ITEM)) {
            CartItem item = new CartItem(getDriver(), element);
            item.setRootElement(element);
            items.add(item);
        }
        return items;
    }

    public List<Product> getItems() {
        resetScrollPosition();
        Map<String, Product> collected = new LinkedHashMap<>();
        for (int pass = ZERO; pass < SIX; pass++) {
            collectInto(collected);
            if (!scrollForMoreItems()) {
                collectInto(collected);
                break;
            }
        }
        resetScrollPosition();
        return new ArrayList<>(collected.values());
    }

    private void collectInto(Map<String, Product> collected) {
        for (CartItem item : findRenderedItems()) {
            String name = readItemName(item);
            if (!name.isEmpty()) {
                collected.putIfAbsent(name, new Product(name, readItemPrice(item)));
            }
        }
    }

    public BigDecimal getSubtotal() {
        revealSummary();
        return amountFrom(readSubtotalLabel());
    }

    public BigDecimal getTax() {
        revealSummary();
        return amountFrom(readTaxLabel());
    }

    public BigDecimal getTotal() {
        revealSummary();
        return amountFrom(readTotalLabel());
    }

    private BigDecimal amountFrom(String label) {
        return new BigDecimal(label.replaceAll("[^0-9.-]", ""));
    }

    public ProductsPage cancel() {
        revealSummary();
        tap(buttonCancel);
        return initPage(getDriver(), ProductsPage.class);
    }

    public CheckoutCompletePage finish() {
        revealSummary();
        tap(buttonFinish);
        return initPage(getDriver(), CheckoutCompletePage.class);
    }

    protected abstract boolean scrollForMoreItems();

    protected abstract void resetScrollPosition();

    protected abstract void revealSummary();

    protected abstract String readItemName(CartItem item);

    protected abstract String readItemPrice(CartItem item);

    protected abstract String readSubtotalLabel();

    protected abstract String readTaxLabel();

    protected abstract String readTotalLabel();
}
