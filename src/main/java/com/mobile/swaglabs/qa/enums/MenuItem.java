package com.mobile.swaglabs.qa.enums;

/**
 * Entries of the hamburger navigation menu, in the order the app renders them.
 *
 * <p>The accessibility id is the label prefixed with {@code test-} on both platforms.
 */
public enum MenuItem {

    ALL_ITEMS("ALL ITEMS"),
    WEBVIEW("WEBVIEW"),
    QR_CODE_SCANNER("QR CODE SCANNER"),
    GEO_LOCATION("GEO LOCATION"),
    DRAWING("DRAWING"),
    ABOUT("ABOUT"),
    LOGOUT("LOGOUT"),
    RESET_APP_STATE("RESET APP STATE");

    private static final String ACCESSIBILITY_ID_PREFIX = "test-";

    private final String label;

    MenuItem(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getAccessibilityId() {
        return ACCESSIBILITY_ID_PREFIX + label;
    }
}
