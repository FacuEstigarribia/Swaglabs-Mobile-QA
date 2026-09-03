package com.mobile.swaglabs.qa.retry;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.mobile.swaglabs.qa.IConstants;

/**
 * Primitive tests that fail on purpose, to exercise {@link RetryCountAnalyzer}.
 */
public class RetryAnalyzerDemoTest implements IConstants {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final ConcurrentMap<String, AtomicInteger> ATTEMPTS = new ConcurrentHashMap<>();

    @DataProvider(name = "rows", parallel = true)
    public Object[][] rows() {
        return new Object[][] { { "row-a" }, { "row-b" }, { "row-c" } };
    }

    @Test(description = "Fails on the first attempt and passes on the retry")
    public void testFailsOnceThenPasses() {
        int attempt = countAttempt("testFailsOnceThenPasses");
        Assert.assertTrue(attempt > ONE,
                String.format("Attempt %d of testFailsOnceThenPasses fails on purpose!", attempt));
    }

    @Test(description = "Fails on every attempt, so the retry budget runs out")
    public void testAlwaysFails() {
        int attempt = countAttempt("testAlwaysFails");
        Assert.fail(String.format("Attempt %d of testAlwaysFails fails on purpose!", attempt));
    }

    @Test(description = "Passes first time, so it is never retried")
    public void testPassesFirstTime() {
        int attempt = countAttempt("testPassesFirstTime");
        Assert.assertEquals(attempt, ONE, "A test that passed was run more than once!");
    }

    @Test(dataProvider = "rows", description = "Every data-provider row gets its own retry budget")
    public void testFailsOncePerRow(String row) {
        int attempt = countAttempt("testFailsOncePerRow-" + row);
        Assert.assertTrue(attempt > ONE,
                String.format("Attempt %d of row '%s' fails on purpose!", attempt, row));
    }

    /** Attempt number of one invocation, counted across its retries, from any thread. */
    private int countAttempt(String invocation) {
        int attempt = ATTEMPTS.computeIfAbsent(invocation, key -> new AtomicInteger(ZERO)).incrementAndGet();
        LOGGER.info("Running '{}': attempt {} on thread '{}'.",
                invocation, attempt, Thread.currentThread().getName());
        return attempt;
    }
}
