package com.mobile.swaglabs.qa.enums;

import java.util.Comparator;

import com.mobile.swaglabs.qa.data.Product;

/**
 * The four options offered by the Products sort selector.
 *
 * <p>Each constant carries both the label the app renders and the ordering that label promises,
 * so a test can sort the observed products with {@link #getExpectedOrder()} and compare, rather
 * than restating the ordering rule at every call site.
 */
public enum SortOption {

    NAME_A_TO_Z("Name (A to Z)", Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)),
    NAME_Z_TO_A("Name (Z to A)", Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER).reversed()),
    PRICE_LOW_TO_HIGH("Price (low to high)", Comparator.comparingDouble(Product::getPrice)),
    PRICE_HIGH_TO_LOW("Price (high to low)", Comparator.comparingDouble(Product::getPrice).reversed());

    private final String label;
    private final Comparator<Product> expectedOrder;

    SortOption(String label, Comparator<Product> expectedOrder) {
        this.label = label;
        this.expectedOrder = expectedOrder;
    }

    /** The exact text the app renders for this option, on both platforms. */
    public String getLabel() {
        return label;
    }

    public Comparator<Product> getExpectedOrder() {
        return expectedOrder;
    }
}
