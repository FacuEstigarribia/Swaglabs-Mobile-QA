package com.mobile.swaglabs.qa;

/**
 * Shared constants for the Swag Labs mobile suite.
 */
public interface IConstants {

    //=================== Numbers ===================//
    int ZERO = 0;
    int ONE = 1;
    int TWO = 2;
    int THREE = 3;
    int FIVE = 5;
    int SIX = 6;
    int THIRTY = 30;

    //=================== Timeouts ==================//
    int DEFAULT_TIMEOUT = FIVE;

    /**
     * Longer wait covering a cold app start.
     * <p>The app is relaunched for every test method and shows a splash screen first, so waiting
     * for the login screen is waiting on app startup rather than on an element. Using
     * {@link #DEFAULT_TIMEOUT} here makes the first assertion of a test flaky.
     */
    int LOGIN_TIMEOUT = THIRTY;

    /** The Swag Labs catalog is a fixed set of six products. */
    int EXPECTED_PRODUCT_COUNT = SIX;

    /** Prices render as {@code $12.34}. */
    String PRICE_PATTERN = "\\$\\d+\\.\\d{2}";

    /** Exact banner text the app shows for credentials it does not recognise. */
    String LOGIN_ERROR_MESSAGE = "Username and password do not match any user in this service.";

    //=================== Page-opened messages ======//
    String LOGIN_PAGE_NOT_OPENED = "Login page is not opened!";
    String PRODUCTS_PAGE_NOT_OPENED = "Products page is not opened!";
    String PRODUCT_DETAILS_PAGE_NOT_OPENED = "Product details page is not opened!";
    String CART_PAGE_NOT_OPENED = "Cart page is not opened!";
    String MENU_NOT_OPENED = "Navigation menu is not opened!";

    //=================== Content messages ==========//
    String PRODUCT_COUNT_MISMATCH = "Unexpected number of products in the grid!";
    String CART_BADGE_MISMATCH = "Cart badge shows an unexpected count!";
    String CART_ITEM_COUNT_MISMATCH = "Unexpected number of items in the cart!";
}
