package com.mobile.swaglabs.qa.retry;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

import com.mobile.swaglabs.qa.IConstants;

/**
 * Gives every test method a {@link RetryCountAnalyzer}, so no test has to declare one.
 */
public class RetryAnalyzerListener implements ITestListener, IConstants {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void onStart(ITestContext context) {
        int maxRetries = RetryCountAnalyzer.resolveMaxRetries(context);
        if (maxRetries <= ZERO) {
            LOGGER.info("'{}' is {}, so failed tests in '{}' are not retried.",
                    RetryCountAnalyzer.RETRY_COUNT, maxRetries, context.getName());
            return;
        }

        ITestNGMethod[] methods = context.getAllTestMethods();
        int attached = ZERO;
        for (ITestNGMethod method : methods) {
            if (declaresNoAnalyzer(method)) {
                method.setRetryAnalyzerClass(RetryCountAnalyzer.class);
                attached++;
            }
        }
        LOGGER.info("Failed tests in '{}' will be retried up to {} time(s); {} of {} methods use {}.",
                context.getName(), maxRetries, attached, methods.length,
                RetryCountAnalyzer.class.getSimpleName());
    }

    /**
     * Whether the method is still free to be given the shared analyzer.
     */
    private boolean declaresNoAnalyzer(ITestNGMethod method) {
        Class<? extends IRetryAnalyzer> declared = method.getRetryAnalyzerClass();
        return declared == null || DisabledRetryAnalyzer.class.equals(declared);
    }
}
