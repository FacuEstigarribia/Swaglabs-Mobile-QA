package com.mobile.swaglabs.qa.listener;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import com.zebrunner.carina.core.registrar.ownership.MethodOwner;
import com.zebrunner.carina.core.registrar.tag.Priority;
import com.zebrunner.carina.core.registrar.tag.TestPriority;
import com.zebrunner.carina.core.registrar.tag.TestTag;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.webdriver.IDriverPool;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;

/**
 * Feeds Allure the metadata every test in this project already declares.
 *
 * <p>The suite annotates each {@code @Test} with {@link MethodOwner}, {@link TestPriority} and two
 * {@link TestTag}s ({@code tcId} and {@code feature}). Those are Zebrunner's annotations, which
 * Allure knows nothing about — so rather than adding a second, parallel set of Allure annotations
 * to all 17 methods, this listener reads the existing ones reflectively and translates them.
 * Adding a test therefore needs no reporting-specific work at all.
 *
 * <p>Registered in {@code src/main/resources/META-INF/services/org.testng.ITestNGListener}, which
 * covers every suite including {@code retry_demo.xml} - the one file with no {@code <listeners>}
 * block.
 *
 * <p><b>Why {@code afterInvocation} and not {@code onTestFailure}.</b> Allure's own
 * {@code AllureTestNg} closes a test case from its {@code ITestListener} callbacks, and the relative
 * order of two {@code ITestListener}s found by the service loader is unspecified — so writing labels
 * from {@code onTestFailure} would be a race that silently drops them. TestNG runs every
 * {@code IInvokedMethodListener.afterInvocation} before those callbacks fire, which makes this hook
 * deterministic at both ends: the test case is open because the method has already run, and it has
 * not been stopped yet.
 */
public class AllureAdapterListener implements IInvokedMethodListener, IDriverPool {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TAG_TC_ID = "tcId";
    private static final String TAG_FEATURE = "feature";

    /** Test cases live in the repo, and {@code docs/test-cases.md} anchors them as {@code #sl-nn}. */
    private static final String TEST_CASE_DOC = "https://github.com/FacuEstigarribia/"
            + "Swaglabs-Mobile-QA/blob/master/docs/test-cases.md#%s";

    private static final String SCREENSHOT_ATTACHMENT = "Failure screenshot";

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!method.isTestMethod()) {
            return;
        }

        AllureLifecycle lifecycle = Allure.getLifecycle();
        if (!lifecycle.getCurrentTestCase().isPresent()) {
            return;
        }

        // Reporting is best-effort: a problem here must never replace the real test result with an
        // unrelated error. Same rule the screenshot listener follows.
        try {
            Method real = testResult.getMethod().getConstructorOrMethod().getMethod();
            lifecycle.updateTestCase(result -> describe(result, real));
        } catch (Exception e) {
            LOGGER.warn("Could not add Allure metadata for '{}'.", testResult.getName(), e);
        }

        // FAILURE specifically, not "not a success": a test skipped because its dependency failed
        // has nothing on screen worth capturing.
        if (testResult.getStatus() == ITestResult.FAILURE) {
            attachScreenshot(lifecycle);
        }
    }

    private void describe(TestResult result, Method method) {
        String tcId = tagValue(method, TAG_TC_ID);
        String feature = tagValue(method, TAG_FEATURE);

        if (feature != null) {
            result.getLabels().add(ResultsUtils.createFeatureLabel(feature));
        }
        if (tcId != null) {
            result.getLabels().add(ResultsUtils.createTagLabel(tcId));
            result.getLinks().add(new Link()
                    .setName(tcId + " test case")
                    .setType("tms")
                    .setUrl(String.format(TEST_CASE_DOC, tcId.toLowerCase(Locale.ROOT))));
        }

        MethodOwner owner = method.getAnnotation(MethodOwner.class);
        if (owner != null && !owner.owner().isEmpty()) {
            result.getLabels().add(ResultsUtils.createOwnerLabel(owner.owner()));
        }

        TestPriority priority = method.getAnnotation(TestPriority.class);
        if (priority != null) {
            result.getLabels().add(ResultsUtils.createSeverityLabel(toSeverity(priority.value())));
        }

        // Groups the Suites tab by platform, so an Android and an iOS run of the same case stay
        // distinguishable when both sets of results sit in one report.
        platform().ifPresent(p -> result.getLabels().add(ResultsUtils.createParentSuiteLabel(p)));

        // Allure names a test after its method by default. The case id and the human title are far
        // more useful, and both are already on the annotations.
        String description = describeTest(method);
        if (description != null) {
            result.setName(tcId == null ? description : tcId + " - " + description);
        }
    }

    private String describeTest(Method method) {
        Test test = method.getAnnotation(Test.class);
        if (test == null || test.description().isEmpty()) {
            return null;
        }
        return test.description();
    }

    /** {@code @TestTag} is repeatable, so a method carries several and each is matched by name. */
    private String tagValue(Method method, String name) {
        for (TestTag tag : method.getAnnotationsByType(TestTag.class)) {
            if (name.equals(tag.name())) {
                return tag.value();
            }
        }
        return null;
    }

    private SeverityLevel toSeverity(Priority priority) {
        switch (priority) {
            case P0:
                return SeverityLevel.BLOCKER;
            case P1:
                return SeverityLevel.CRITICAL;
            case P2:
                return SeverityLevel.NORMAL;
            case P3:
                return SeverityLevel.MINOR;
            default:
                return SeverityLevel.TRIVIAL;
        }
    }

    private Optional<String> platform() {
        String platform = R.CONFIG.get("capabilities.platformName");
        if (platform == null || platform.isEmpty() || "NULL".equalsIgnoreCase(platform)) {
            return Optional.empty();
        }
        return Optional.of("IOS".equalsIgnoreCase(platform) ? "iOS" : "Android");
    }

    /**
     * Attaches the failing screen to the report.
     *
     * <p>Separate from {@link ScreenshotOnFailureListener}, which keeps writing PNGs to
     * {@code target/screenshots/} from {@code onTestFailure}. Keeping the two paths independent
     * costs one extra capture on a failing test and means neither reporter can break the other.
     */
    private void attachScreenshot(AllureLifecycle lifecycle) {
        // Carina's getDriver() *starts* a session when the thread has none, so a driverless suite
        // (retry_demo) would try to open one purely to report a failure. Ask first.
        if (!isDriverRegistered(IDriverPool.DEFAULT)) {
            return;
        }
        try {
            byte[] png = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
            lifecycle.addAttachment(SCREENSHOT_ATTACHMENT, "image/png", "png", png);
        } catch (Exception e) {
            LOGGER.warn("Could not attach a failure screenshot to the Allure report.", e);
        }
    }
}
