package com.mobile.swaglabs.qa.data;

import java.util.Objects;

/**
 * An immutable snapshot of one catalog entry, read off the screen.
 *
 * <p>Product cards are re-rendered whenever the grid is sorted or toggled, which invalidates the
 * underlying elements. Capturing name and price into a value object lets a test compare what it
 * saw before an action with what it sees after, without holding stale element references.
 */
public class Product {

    private final String name;
    private final String priceLabel;

    public Product(String name, String priceLabel) {
        this.name = name;
        this.priceLabel = priceLabel;
    }

    public String getName() {
        return name;
    }

    /** The price exactly as rendered, e.g. {@code $29.99}. */
    public String getPriceLabel() {
        return priceLabel;
    }

    /** The price as a number, for ordering assertions. */
    public double getPrice() {
        return Double.parseDouble(priceLabel.replace("$", "").replace(",", "").trim());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product)) {
            return false;
        }
        Product that = (Product) other;
        return Objects.equals(name, that.name) && Objects.equals(priceLabel, that.priceLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, priceLabel);
    }

    @Override
    public String toString() {
        return name + " (" + priceLabel + ")";
    }
}
