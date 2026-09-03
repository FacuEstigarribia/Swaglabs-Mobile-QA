package com.mobile.swaglabs.qa.retry;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestContext;
import org.testng.ITestResult;

import com.mobile.swaglabs.qa.IConstants;
import com.zebrunner.carina.utils.R;

/**
 * Re-runs a failed test method up to {@code retry_count} times.
 */
public class RetryCountAnalyzer implements IRetryAnalyzer, IConstants {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /** Name of the retry parameter — the one Carina already defines in {@code _config.properties}. */
    static final String RETRY_COUNT = "retry_count";

    /** Retries already spent, per test invocation. Only failing invocations ever get an entry. */
    private static final ConcurrentMap<String, AtomicInteger> RETRIES_SPENT = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        int maxRetries = resolveMaxRetries(result.getTestContext());
        if (maxRetries <= ZERO) {
            return false;
        }

        String invocation = buildKey(result);
        int retryNumber = RETRIES_SPENT.computeIfAbsent(invocation, key -> new AtomicInteger(ZERO))
                .incrementAndGet();

        if (retryNumber > maxRetries) {
            // Forget the invocation, so that a genuine second run of the same method and parameters
            // (invocationCount > 1) starts with a full budget instead of inheriting this one.
            RETRIES_SPENT.remove(invocation);
            LOGGER.error("'{}' failed {} times in a row; giving up.", invocation, maxRetries + ONE);
            return false;
        }

        LOGGER.warn("'{}' failed; retrying it ({} of {}).", invocation, retryNumber, maxRetries);
        return true;
    }

    /**
     * Number of retries a failed test is allowed.
     */
    static int resolveMaxRetries(ITestContext context) {
        Integer fromCommandLine = parse(System.getProperty(RETRY_COUNT), "the command line");
        if (fromCommandLine != null) {
            return fromCommandLine;
        }
        if (context != null && context.getCurrentXmlTest() != null) {
            // XmlTest.getParameter falls back to the suite's parameters, so this covers both levels.
            Integer fromSuite = parse(context.getCurrentXmlTest().getParameter(RETRY_COUNT), "the suite XML");
            if (fromSuite != null) {
                return fromSuite;
            }
        }
        return R.CONFIG.getInt(RETRY_COUNT);
    }

    private static Integer parse(String value, String source) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Ignoring '{}={}' from {}: it is not a whole number.", RETRY_COUNT, value, source);
            return null;
        }
    }

    /** Identifies one test invocation: the class, the method, and the parameters it ran with. */
    private static String buildKey(ITestResult result) {
        Object[] parameters = result.getParameters();
        String arguments = (parameters == null || parameters.length == ZERO)
                ? ""
                : Arrays.deepToString(parameters);
        return String.format("%s.%s%s", result.getTestClass().getRealClass().getName(),
                result.getMethod().getMethodName(), arguments);
    }
}
