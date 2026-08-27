package com.mobile.swaglabs.qa.pages.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.mobile.swaglabs.qa.components.common.ProductCard;
import com.mobile.swaglabs.qa.data.Product;
import com.mobile.swaglabs.qa.enums.SortOption;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.decorator.PageOpeningStrategy;
import com.zebrunner.carina.webdriver.locator.ExtendedFindBy;

import io.appium.java_client.AppiumBy;

/** The product catalog, and the landing page after a successful login. */
public abstract class ProductsPage extends SwagLabsAbstractPage {

    @ExtendedFindBy(accessibilityId = "test-PRODUCTS")
    protected ExtendedWebElement productsContainer;

    @ExtendedFindBy(accessibilityId = "test-Cart drop zone")
    protected ExtendedWebElement headerBar;

    @ExtendedFindBy(accessibilityId = "test-Menu")
    protected ExtendedWebElement buttonMenu;

    @ExtendedFindBy(accessibilityId = "test-Cart")
    protected ExtendedWebElement buttonCart;

    @ExtendedFindBy(accessibilityId = "test-Toggle")
    protected ExtendedWebElement buttonViewToggle;

    @ExtendedFindBy(accessibilityId = "test-Modal Selector Button")
    protected ExtendedWebElement buttonSortSelector;

    private static final By CARD = AppiumBy.accessibilityId("test-Item");

    /** Upper bound on collection passes, so a scrolling problem fails fast instead of looping. */
    private static final int MAX_PASSES = 8;

    protected ProductsPage(WebDriver driver) {
        super(driver);
        setUiLoadedMarker(productsContainer);
        setPageOpeningStrategy(PageOpeningStrategy.BY_ELEMENT);
    }

    /**
     * The product cards currently on screen and fully rendered.
     */
    public List<ProductCard> findRenderedCards() {
        List<ProductCard> cards = new ArrayList<>();
        for (WebElement element : getDriver().findElements(CARD)) {
            ProductCard card = new ProductCard(getDriver(), element);
            // The two-arg constructor only sets the search context for the card's child locators;
            // the card's own root has to be set too, or any operation on the card itself fails
            // with "Both 'By' and 'WebElement' could not be null".
            card.setRootElement(element);
            if (card.isFullyRendered()) {
                cards.add(card);
            }
        }
        return cards;
    }

    /**
     * Scrolls the grid to bring more products into view.
     *
     * @return {@code true} while there was more to scroll. Platforms that render the whole catalog
     *         at once return {@code false} immediately.
     */
    protected abstract boolean scrollForMoreProducts();

    /**
     * Returns the grid to the top.
     *
     * <p>Collecting the catalog leaves the grid scrolled to the bottom, so a test that reads it
     * twice — before and after some action — would otherwise find only the last row the second
     * time. Platforms that never scroll implement this as a no-op.
     */
    protected abstract void resetScrollPosition();

    /**
     * Every product in the catalog, in display order.
     *
     * <p>Scrolls to the end of the list rather than stopping once
     * {@link #EXPECTED_PRODUCT_COUNT} products have been seen. Stopping at the expected count
     * would make the count assertion in SL-01 self-fulfilling: a seventh product could never be
     * reported. Keyed by name in a {@link LinkedHashMap} so display order is kept and the overlap
     * between passes collapses.
     */
    public List<Product> getAllProducts() {
        resetScrollPosition();
        Map<String, Product> collected = new LinkedHashMap<>();

        for (int pass = ZERO; pass < MAX_PASSES; pass++) {
            int before = collected.size();
            collectInto(collected);
            boolean gained = collected.size() > before;
            boolean moved = scrollForMoreProducts();

            // Stop only once a pass both learns nothing new *and* cannot scroll further. Stopping
            // on the scroll signal alone loses cards that were still half-rendered when the
            // gesture reported the end of the list.
            if (!gained && !moved) {
                collectInto(collected);
                break;
            }
        }

        LOGGER.info("Collected {} product(s) from the grid.", collected.size());
        return new ArrayList<>(collected.values());
    }

    private void collectInto(Map<String, Product> collected) {
        for (ProductCard card : findRenderedCards()) {
            Product product = card.toProduct();
            collected.putIfAbsent(product.getName(), product);
        }
    }

    public List<String> getAllProductNames() {
        return getAllProducts().stream().map(Product::getName).collect(Collectors.toList());
    }

    /** Opens the sort selector and picks {@code option}. */
    public abstract void sortBy(SortOption option);

    /** The number on the cart badge, or 0 when no badge is shown. */
    public abstract int getCartBadgeCount();

    public boolean isCartBadgeDisplayed() {
        return getCartBadgeCount() > ZERO;
    }

    public boolean isProductsTitleDisplayed() {
        return headerBar.isElementPresent(DEFAULT_TIMEOUT);
    }

    public boolean isViewToggleDisplayed() {
        return buttonViewToggle.isElementPresent(DEFAULT_TIMEOUT);
    }

    /** Toggles between the grid and list layouts. */
    public void toggleView() {
        LOGGER.info("Toggling the product view layout.");
        tap(buttonViewToggle);
    }

    /**
     * Adds the product at {@code index} of the catalog and returns it.
     *
     * <p>The grid is rewound first so {@code index} counts from the top of the catalog regardless
     * of where a previous call left it scrolled.
     */
    public Product addProductToCart(int index) {
        resetScrollPosition();
        ProductCard card = findRenderedCards().get(index);
        Product product = card.toProduct();
        LOGGER.info("Adding '{}' to the cart.", product);
        tap(card.getAddToCartButton());
        return product;
    }

    /**
     * Adds {@code count} distinct products, scrolling when the rendered cards run out.
     *
     * <p>Android renders only two cards at a time, so adding three means scrolling; tracking names
     * rather than indices keeps that from re-adding a card that is still on screen after a scroll.
     */
    public List<Product> addProductsToCart(int count) {
        resetScrollPosition();
        List<Product> added = new ArrayList<>();
        Set<String> addedNames = new LinkedHashSet<>();

        for (int pass = ZERO; pass < MAX_PASSES && added.size() < count; pass++) {
            for (ProductCard card : findRenderedCards()) {
                if (added.size() == count) {
                    break;
                }
                String name = card.getProductName();
                if (addedNames.contains(name) || !card.isAddToCartDisplayed()) {
                    continue;
                }
                Product product = card.toProduct();
                LOGGER.info("Adding '{}' to the cart.", product);
                tap(card.getAddToCartButton());

                // Count it only once the app confirms it, by flipping the card to REMOVE. A tap
                // issued while the grid is still settling after a scroll can be swallowed, and
                // recording it regardless would report a product as added that never reached the
                // cart — the failure would then surface later as a confusing count mismatch.
                // Asked of the card just tapped, which is far cheaper than re-scanning the grid.
                if (card.isRemoveDisplayed()) {
                    added.add(product);
                    addedNames.add(name);
                } else {
                    LOGGER.warn("Tap on ADD TO CART for '{}' did not register; will retry.", name);
                }
            }
            if (added.size() < count && !scrollForMoreProducts()) {
                break;
            }
        }

        if (added.size() < count) {
            LOGGER.warn("Only {} of the {} requested products could be added.", added.size(), count);
        }
        return added;
    }

    /** Whether the card for {@code product} currently shows REMOVE rather than ADD TO CART. */
    public boolean isRemoveButtonDisplayedFor(Product product) {
        for (ProductCard card : findRenderedCards()) {
            if (card.getProductName().equals(product.getName())) {
                return card.isRemoveDisplayed();
            }
        }
        return false;
    }

    /**
     * Opens the details screen for the product at {@code index} of the catalog.
     *
     * <p>The grid is rewound first so {@code index} counts from the top of the catalog regardless
     * of where a previous call left it scrolled.
     */
    public ProductDetailsPage openProduct(int index) {
        resetScrollPosition();
        ProductCard card = findRenderedCards().get(index);
        LOGGER.info("Opening product details for '{}'.", card.getProductName());
        tap(card.getNameElement());
        return initPage(getDriver(), ProductDetailsPage.class);
    }

    public CartPage openCart() {
        LOGGER.info("Opening the cart.");
        tap(buttonCart);
        return initPage(getDriver(), CartPage.class);
    }

    public MenuPage openMenu() {
        LOGGER.info("Opening the navigation menu.");
        tap(buttonMenu);
        return initPage(getDriver(), MenuPage.class);
    }
}
