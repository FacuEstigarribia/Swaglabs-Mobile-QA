package com.mobile.swaglabs.qa.listener;

import java.io.Serializable;
import java.util.UUID;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;

/**
 * Turns this project's own log lines into Allure report steps.
 */
@Plugin(name = AllureStepAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class AllureStepAppender extends AbstractAppender {

    static final String PLUGIN_NAME = "AllureStep";

    private AllureStepAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @PluginFactory
    public static AllureStepAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new AllureStepAppender(name == null ? PLUGIN_NAME : name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        AllureLifecycle lifecycle = Allure.getLifecycle();

        // Logging happens outside a test too - suite setup, capability loading, teardown. There is
        // no test case to attach a step to then, and asking Allure to start one would only produce
        // a warning per line.
        if (!lifecycle.getCurrentTestCase().isPresent()) {
            return;
        }

        try {
            String uuid = UUID.randomUUID().toString();
            lifecycle.startStep(uuid, new StepResult()
                    .setName(event.getMessage().getFormattedMessage())
                    .setStatus(Status.PASSED));
            lifecycle.stopStep(uuid);
        } catch (Exception e) {
            // An appender must never throw: it would surface as a logging failure in the middle of
            // an unrelated test. Reporting is best-effort by design.
            error("Could not record an Allure step for a log event.", event, e);
        }
    }
}
