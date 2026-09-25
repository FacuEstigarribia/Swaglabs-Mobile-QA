package com.mobile.swaglabs.qa.pages.ios;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.components.common.CartItem;
import com.mobile.swaglabs.qa.pages.common.CheckoutOverviewPage;
import com.zebrunner.carina.utils.factory.DeviceType;
import com.zebrunner.carina.utils.factory.DeviceType.Type;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;

import io.appium.java_client.AppiumBy;

@DeviceType(pageType = Type.IOS_PHONE, parentClass = CheckoutOverviewPage.class)
public class IOSCheckoutOverviewPage extends CheckoutOverviewPage {

    private static final By CHILD_TEXTS = By.xpath(".//XCUIElementTypeStaticText");
    private static final By OVERVIEW_CONTENT =
            AppiumBy.accessibilityId("test-CHECKOUT: OVERVIEW");
    private static final String SUMMARY_XPATH = "//*[starts-with(@name, 'test-%s')]";

    public IOSCheckoutOverviewPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void tap(ExtendedWebElement element) {
        IOSTouch.tap(getDriver(), element);
    }

    @Override
    protected boolean scrollForMoreItems() {
        String before = renderedSignature();
        IOSScroll.down(getDriver(), OVERVIEW_CONTENT);
        return !renderedSignature().equals(before);
    }

    @Override
    protected void resetScrollPosition() {
        for (int attempt = ZERO; attempt < SIX; attempt++) {
            String before = renderedSignature();
            IOSScroll.up(getDriver(), OVERVIEW_CONTENT);
            if (renderedSignature().equals(before)) {
                return;
            }
        }
    }

    @Override
    protected void revealSummary() {
        for (int attempt = ZERO; attempt < SIX; attempt++) {
            if (!getDriver().findElements(summaryBy("Item total: ")).isEmpty()) {
                return;
            }
            IOSScroll.down(getDriver(), OVERVIEW_CONTENT);
        }
    }

    @Override
    protected String readItemName(CartItem item) {
        List<ExtendedWebElement> texts = textsIn(item.getDescription());
        if (texts.size() < TWO) {
            return "";
        }
        return textOf(texts.get(ZERO));
    }

    @Override
    protected String readItemPrice(CartItem item) {
        List<ExtendedWebElement> texts = textsIn(item.getPrice());
        return texts.isEmpty() ? textOf(item.getPrice()) : textOf(texts.get(ZERO));
    }

    @Override
    protected String readSubtotalLabel() {
        return readSummaryLabel("Item total: ");
    }

    @Override
    protected String readTaxLabel() {
        return readSummaryLabel("Tax: ");
    }

    @Override
    protected String readTotalLabel() {
        return readSummaryLabel("Total: ");
    }

    private String renderedSignature() {
        StringBuilder signature = new StringBuilder();
        for (CartItem item : findRenderedItems()) {
            signature.append(readItemName(item)).append('|');
        }
        return signature.toString();
    }

    private String readSummaryLabel(String prefix) {
        ExtendedWebElement label = findExtendedWebElement(summaryBy(prefix), DEFAULT_TIMEOUT);
        String text = textOf(label);
        if (containsAmount(text)) {
            return text;
        }
        for (ExtendedWebElement child : textsIn(label)) {
            text = textOf(child);
            if (containsAmount(text)) {
                return text;
            }
        }
        String accessibilityLabel = label.getAttribute("name");
        return accessibilityLabel == null || accessibilityLabel.isEmpty()
                ? text
                : accessibilityLabel.replaceFirst("^test-", "");
    }

    private boolean containsAmount(String text) {
        return text.matches(".*\\d+\\.\\d{2}.*");
    }

    private By summaryBy(String prefix) {
        return By.xpath(String.format(SUMMARY_XPATH, prefix));
    }

    private String textOf(ExtendedWebElement element) {
        String text = element.getText();
        return text == null ? "" : text.trim();
    }

    private List<ExtendedWebElement> textsIn(ExtendedWebElement container) {
        if (!container.isElementPresent(DEFAULT_TIMEOUT)) {
            return Collections.emptyList();
        }
        return findExtendedWebElements(container, CHILD_TEXTS, Duration.ofSeconds(ONE));
    }
}
