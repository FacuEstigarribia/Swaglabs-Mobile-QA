package com.mobile.swaglabs.qa.pages.common;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import com.mobile.swaglabs.qa.IConstants;
import com.zebrunner.carina.utils.mobile.IMobileUtils;
import com.zebrunner.carina.webdriver.decorator.ExtendedWebElement;
import com.zebrunner.carina.webdriver.gui.AbstractPage;

/**
 * Base for every Swag Labs screen.
 */
public abstract class SwagLabsAbstractPage extends AbstractPage implements IConstants, IMobileUtils {

    protected static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    protected SwagLabsAbstractPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Taps an element. The default is a plain click; platforms whose reported coordinates do not
     * match the touch space override this.
     */
    public void tap(ExtendedWebElement element) {
        element.click();
    }
}
