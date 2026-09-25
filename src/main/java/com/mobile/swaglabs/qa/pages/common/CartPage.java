package com.mobile.swaglabs.qa.pages.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.mobile.swaglabs.qa.components.common.CartItem;
import com.mobile.swaglabs.qa.data.Product;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

import io.appium.java_client.AppiumBy;

/** The cart screen. */
public abstract class CartPage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-Cart Content")
    protected ExtendedWebElement cartContent;

    @ExtendedFindBy(accessibilityId = "test-CONTINUE SHOPPING")
    protected ExtendedWebElement buttonContinueShopping;

    @ExtendedFindBy(accessibilityId = "test-CHECKOUT")
    protected ExtendedWebElement buttonCheckout;

    private static final By ITEM = AppiumBy.accessibilityId("test-Item");

    /** Upper bound on collection passes, so a scrolling problem fails fast instead of looping. */
    private static final int MAX_PASSES = 8;

    /** Taps at a removal before giving up, since the app drops taps during a re-render. */
    private static final int REMOVE_ATTEMPTS = 3;

    /** Collection attempts before accepting a short read of the cart. */
    private static final int COLLECT_ATTEMPTS = 3;

    protected CartPage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(cartContent);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    /**
     * The line items currently on screen.
     */
    protected List<CartItem> findRenderedItems() {
        List<CartItem> items = new ArrayList<>();
        for (WebElement element : getDriver().findElements(ITEM)) {
            CartItem item = new CartItem(getDriver(), element);
            // The two-arg constructor only sets the search context for child locators; the
            // component's own root has to be set too.
            item.setRootElement(element);
            items.add(item);
        }
        return items;
    }

    /**
     * Scrolls the cart to bring more line items into view.
     *
     * @return {@code true} while there was more to scroll
     */
    protected abstract boolean scrollForMoreItems();

    /** Returns the cart to the top, so a repeated read starts from a known position. */
    protected abstract void resetScrollPosition();

    /**
     * Brings CONTINUE SHOPPING and CHECKOUT into view.
     */
    protected abstract void revealActions();

    /**
     * Every line item in the cart, in display order.
     */
    public List<Product> getItems() {
        // The badge is the app's own count, so it is the yardstick for a complete read. Without
        // this check a row that was slow to render is silently dropped, and two consecutive reads
        // of the same cart can disagree.
        int expected = getCartBadgeCount();
        List<Product> items = Collections.emptyList();

        for (int attempt = ZERO; attempt < COLLECT_ATTEMPTS; attempt++) {
            items = collectAllItems();
            if (expected == ZERO || items.size() >= expected) {
                return items;
            }
            LOGGER.warn("Read {} cart line item(s) but the badge says {}; re-reading.",
                    items.size(), expected);
        }

        LOGGER.error("Cart read settled at {} line item(s) against a badge of {}.",
                items.size(), expected);
        return items;
    }

    /** One full scroll-through of the cart. */
    private List<Product> collectAllItems() {
        resetScrollPosition();
        Map<String, Product> collected = new LinkedHashMap<>();

        for (int pass = ZERO; pass < MAX_PASSES; pass++) {
            collectInto(collected);
            if (!scrollForMoreItems()) {
                // The gesture that reports "no more" still moves the list, so collect once more.
                collectInto(collected);
                break;
            }
        }

        LOGGER.info("Collected {} cart line item(s).", collected.size());
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

    /** The line item's product name. Platforms nest it differently within the description block. */
    protected abstract String readItemName(CartItem item);

    /** The line item's price, exactly as rendered. */
    protected abstract String readItemPrice(CartItem item);

    public abstract int getCartBadgeCount();

    public int getItemCount() {
        return getItems().size();
    }

    public boolean isEmpty() {
        return getItems().isEmpty();
    }

    public List<String> getItemNames() {
        return getItems().stream().map(Product::getName).collect(Collectors.toList());
    }

    public boolean isCheckoutDisplayed() {
        revealActions();
        return buttonCheckout.isElementPresent(DEFAULT_TIMEOUT);
    }

    /**
     * Removes the line item at {@code index} of the cart.
     */
    public void removeItem(int index) {
        resetScrollPosition();
        String name = readItemName(findRenderedItems().get(index));
        LOGGER.info("Removing '{}' from the cart.", name);

        for (int attempt = ZERO; attempt < REMOVE_ATTEMPTS; attempt++) {
            if (attempt > ZERO) {
                LOGGER.warn("Tap on REMOVE for '{}' did not register; retry {} of {}.",
                        name, attempt, REMOVE_ATTEMPTS - ONE);
            }
            if (!tapRemoveFor(name)) {
                return;
            }
            if (waitForRemoval(name)) {
                return;
            }
        }
        LOGGER.error("'{}' is still in the cart after {} REMOVE attempts.", name, REMOVE_ATTEMPTS);
    }

    /**
     * Taps REMOVE on the row for {@code name}.
     * @return {@code false} when the row is already gone, so there is nothing left to do
     */
    private boolean tapRemoveFor(String name) {
        for (CartItem candidate : findRenderedItems()) {
            if (name.equals(readItemName(candidate))) {
                tap(candidate.getRemoveButton());
                return true;
            }
        }
        return false;
    }

    /** Polls briefly for {@code name} to leave the cart. */
    private boolean waitForRemoval(String name) {
        for (int attempt = ZERO; attempt < THREE; attempt++) {
            if (!getItemNames().contains(name)) {
                return true;
            }
            pause(ONE);
        }
        return false;
    }

    public ProductsPage continueShopping() {
        LOGGER.info("Continuing shopping.");
        revealActions();
        tap(buttonContinueShopping);
        return initPage(getDriver(), ProductsPage.class);
    }

    public CheckoutInformationPage checkout() {
        LOGGER.info("Starting checkout.");
        revealActions();
        tap(buttonCheckout);
        return initPage(getDriver(), CheckoutInformationPage.class);
    }
}
