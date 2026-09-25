# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# QAMobileSwaglabs — working notes

Carina 1.3.0 + Appium UI automation for the Swag Labs mobile app, running one suite against Android
and iOS. JDK 11, TestNG, no unit tests — every test drives
a real device or simulator through a local Appium server at `http://localhost:4723`.

## Commands

```bash
mvn clean test -Dsuite=regression   
mvn clean test -Dsuite=ios_regression  
mvn clean test -Dsuite=smoke        
mvn clean test -Dsuite=ios_smoke    
mvn clean test -Dsuite=grid         
mvn clean test -Dsuite=ios_grid 
mvn allure:serve                   
mvn clean test -Dsuite=retry_demo -Dthread_count=4 -Ddata_provider_thread_count=3  
python3 docs/generate_readme_cases.py   
python3 docs/generate_zebrunner_import.py   
```

`-Dsuite=X` selects `src/test/resources/testng_suites/X.xml`; the default is `android` (`pom.xml`).
Any capability can be overridden from the command line and wins over the properties file:
`-Dcapabilities.deviceName=emulator-5556`, `-Dcapabilities.udid=<SIM-UDID>`.

**To run a subset, narrow a suite file — don't reach for `-Dtest=`.** Surefire here is bound to
`suiteXmlFiles`, and `-Dtest=` makes it ignore them, which drops the `platform` suite parameter that
`SwagLabsBaseTest.loadPlatformCapabilities` requires. Add a `<methods><include name="..."/></methods>`
block to a suite (`smoke.xml` shows the shape) or add a new suite XML.

Stop a run with `pkill -f ForkedBooter` (the surefire JVM is named `ForkedBooter`, so
`pkill -f surefire` does *not* match it). Killing a run mid-flight orphans the Appium session; see
the README for recovering a wedged UiAutomator2 server.

Artifacts: `target/screenshots/` (failure PNGs), `target/logs/test.log`, `target/reports/`,
`target/allure-results/` (+ `target/allure-report/` after `mvn allure:report`).

## Layering

`Tests → Services → Pages → Components`

- **Tests** (`src/test/java/.../test/`) hold steps and assertions only. Five classes, one per area.
- **Services** (`service/`) own multi-page workflows. `LoginService` is the only one so far;
  `UsersPool` also lives here.
- **Pages** (`pages/`) own UI interaction, no business logic.
- **Components** (`components/`) model repeated blocks — `ProductCard`, `CartItem`.

## Page objects

Abstract page in `pages/common/`, platform implementations in `pages/android/` and `pages/ios/`
annotated `@DeviceType(pageType = …, parentClass = …)`. Tests declare the abstract type and
`initPage(...)` resolves the implementation.

Note that `@DeviceType` requires each platform page to extend the *common* page, so there is no
room for a per-platform base class. Shared platform code therefore lives in small static helpers
(`AndroidScroll`, `AndroidCartBadge`, `IOSScroll`, `IOSTouch`, `IOSCartBadge`), not in a superclass.

Locators are `@ExtendedFindBy(accessibilityId = …)` wherever possible — the app uses the same ids
on both platforms. Only relative XPaths differ, because Android matches on `content-desc` and iOS
on `name`.

`SwagLabsAbstractPage.tap(element)` is the seam for platform touch handling: it is a plain
`click()` by default and every iOS page overrides it to route through `IOSTouch`. **New page code
must go through `tap(...)`, never `element.click()`** — a direct click on iOS misses letterboxed
header controls silently.

## Runtime wiring

Worth knowing before changing configuration, because it is spread across four files:

- **Capabilities.** Each suite XML declares `<parameter name="platform" value="android|ios"/>`;
  `SwagLabsBaseTest.loadPlatformCapabilities` (`@BeforeSuite`) loads
  `src/main/resources/capabilities/<platform>.properties` into `R.CONFIG`. Carina does not read
  `capabilities.*` out of TestNG suite parameters, which is why the indirection exists.
  Four layers, in order: the platform file (farm-safe caps only), then
  `capabilities/<platform>-local.properties` (this machine's `deviceName`/`udid`/`platformVersion`)
  **unless** `provider` is set, then any `-Dcapabilities.*`, then the capabilities a Zebrunner
  launcher picked. The last two are re-applied here because the properties load happens after JVM
  start and after `CarinaListener`'s constructor respectively, so without it the file would win over
  both. `_config.properties` stays platform-neutral.
- **Driver lifecycle.** `driver_mode=method_mode`, so Carina relaunches the app per test *method*:
  every test starts with an empty cart and no session, which is what makes rule 1 cheap. It is
  *not* per data-provider invocation — SL-16 logs out explicitly between users for that reason.
- **Retries.** `retry/RetryCountAnalyzer` (an `IRetryAnalyzer`) re-runs a failed method up to
  `retry_count` times; `retry/RetryAnalyzerListener` attaches it to every method in
  `onStart(ITestContext)`, so no `@Test` names an analyzer. That listener is registered in
  `src/main/resources/META-INF/services/org.testng.ITestNGListener`, i.e. TestNG finds it by itself
  and the logic covers every suite including new ones. Count resolution is `-Dretry_count=N` >
  suite/`<test>` XML parameter > `_config.properties` (`0`, so retrying is off by default and the
  listener then attaches nothing). Attempts are counted in a static `ConcurrentMap` keyed by class +
  method + parameters, not in an instance field, so parallel threads and data-provider rows each get
  their own budget regardless of how many analyzer instances TestNG decides to create.
  `retry_demo.xml` + `RetryAnalyzerDemoTest` exercise it with no device and are *meant to end red*.
- **Allure reporting.** Three pieces, none of which touch a test class. `allure-testng` registers
  itself through its own copy of the `META-INF/services/org.testng.ITestNGListener` file, so it is
  active in every suite. `listener/AllureStepAppender` is a log4j2 appender wired to the
  `com.mobile.swaglabs.qa` logger at INFO in `log4j2.xml`, turning the pages' and services' existing
  `LOGGER.info` narration into report steps — which is why no `@Step` annotation and no AspectJ
  weaver exist here. `listener/AllureAdapterListener` reads `@TestTag`/`@TestPriority`/`@MethodOwner`
  reflectively and translates them into Allure's feature/severity/owner/tag labels, so new tests need
  no reporting-specific annotations at all. `categories.json` is copied into the results directory by
  `maven-resources-plugin`; `environment.properties` is written by `SwagLabsBaseTest` once the
  capabilities are loaded.
- **Users.** `_testdata.properties` declares pools (`::`-separated member keys, each resolving to
  `<key>.login` / `<key>.password`). The `UserPool` enum implements `UserProvider`, so a pool
  constant goes straight into `getLoginService().login(VALID_USERS_POOL)`. `UsersPool.getUser()`
  leases per thread and returns the *same* user on a repeat call for that pool;
  `getAllUsers()` reads a pool without leasing it, for data providers; `SwagLabsBaseTest`
  releases in `@AfterMethod`.
- **Suites list classes explicitly.** A new test class must be added to both `android.xml` and
  `ios.xml`, and the `ScreenshotOnFailureListener` is registered per suite XML too.
- **Test metadata.** Every `@Test` carries `@MethodOwner`, `@TestPriority`, and
  `@TestTag(tcId=SL-nn)` + `@TestTag(feature=…)`. `docs/test-cases-formatted.csv` is the source of
  truth for the case design; the README and both Zebrunner imports are generated from it.
- **Zebrunner.** Three separate things, none of which needs a new dependency — `carina-core` already
  brings `agent-core`, so `agent.properties`, `@TestCaseKey` and `RemoteWebDriverFactory` are on the
  classpath.
  - *Reporting.* `src/main/resources/agent.properties` holds hostname and
    `reporting.project-key=SAUCEM`, with `reporting.enabled=false` so a local run stays local. The
    access token is deliberately not in the file; pass it as `REPORTING_SERVER_ACCESS_TOKEN`. A
    launcher run gets all three injected as `REPORTING_*` environment variables, which outrank the
    file.
  - *Launchers.* Zebrunner's repository scan generates one job per TestNG suite XML under
    `src/test/resources/testng_suites/`, and takes the project from `reporting.project-key` in
    `agent.properties`. What the generated job looks like comes from suite `<parameter>`s that Carina
    itself never reads: `jenkinsJobName`, `jenkinsJobType` (`android`/`ios` — it decides the default
    `capabilities` field), `jenkinsMobileDefaultPool`, `provider`, `suiteOwner`, `jenkinsEmail`,
    `jenkinsEnvironments`, and `stringParam::<name>::<description>` for an extra launcher field. The
    scan cannot be told to skip a suite; `jenkinsJobDisabled=true` is how a generated job is
    neutralised, and no suite uses it. Adding `jenkinsRegressionPipeline` would additionally
    generate a cron job; none do.
    `retry_demo.xml` is the launcher to run first: it drives no device (`jenkinsJobType=api`, no
    `provider`), so it exercises the scan, job generation, reporting and retry tracking without a
    device farm. It is meant to end red, and does so as failed tests rather than a broken build
    because the pipeline passes `-Dmaven.test.failure.ignore=true` whenever reporting is on. Its
    `jenkinsDefaultRetryCount=2` matters: the launcher always passes `-Dretry_count`, which
    outranks the suite's own `retry_count` parameter.
  - *Case linkage.* `@TestCaseKey("SAUCEM-nn")` on a `@Test` reports its result against the imported
    TCM case. Prefer the annotation over `TestCase.setTestCaseKey` from a listener: the agent reads
    it when it starts the test, whereas a second `IInvokedMethodListener` would be racing it.
  - *Case migration.* `docs/generate_zebrunner_import.py` reshapes
    `docs/test-cases-formatted.csv` into `docs/zebrunner-test-cases.csv` for the TCM importer by
    expanding its numbered multiline steps into one row per step. The alternate
    `docs/test_cases_formatted_import.py` keeps one row per case and renames `Step` to
    `Step Action`.

## Rules

1. Every test method logs in independently. No shared session.
2. Whenever a page opens, assert it with a hard `Assert.assertTrue(page.isPageOpened(DEFAULT_TIMEOUT), X_PAGE_NOT_OPENED)`.
   The sole exception is **SL-17**, which asserts the opposite on purpose; it is commented as such.
3. `SoftAssert` groups only assertions unrelated to page opening, and always ends in `assertAll()`.
4. Assertion messages end with `!` and name the subject. Promote to an `IConstants` constant once
   used more than twice.
5. Numeric literals live in `IConstants`, in numeric order.

## Gotchas worth remembering

- **Never hold a page-level `List<ProductCard>`.** Carina binds it to the elements found at page
  init, and it goes stale the moment the grid scrolls. Use `findRenderedCards()`, which re-queries.
- **Carina proxies drivers and elements.** Casting to `RemoteWebDriver` or `RemoteWebElement`
  throws. Cast to the interface (`Interactive`, `JavascriptExecutor`) or unwrap via `WrapsElement`.
- **Building an `AbstractUIObject` by hand needs two steps.** `new ProductCard(driver, element)`
  only sets the search context for the child locators; you must also call `setRootElement(element)`
  or any operation on the component itself fails with
  `Both 'By' and 'WebElement' could not be null`.
- **Both platforms virtualize lists.** Off-screen content is unusable — this affects the product
  grid, the cart's line items, *and* the cart's action buttons. Anything that reads a whole list
  has to scroll and accumulate. (iOS looks like it exposes everything in the page source; it does
  not — that assumption cost a debugging cycle, and the README's platform-differences table is
  still stale on this point.)
- **The two scroll helpers report differently.** `AndroidScroll` (`mobile: scrollGesture`) returns
  whether more content remains; `IOSScroll` (`mobile: scroll`) returns nothing, so iOS pages detect
  the end of a list by comparing a `renderedSignature()` before and after the gesture.
- **The app swallows taps issued while a list is re-rendering.** After adding to or removing from
  a list, confirm the app actually acted (the card flipped to REMOVE, the row disappeared) before
  moving on. Assuming the tap landed produces a confusing failure several steps later.
- **`test-Item` is the id of *both* a product-grid card and a cart line item.** Code that collects
  one must tolerate matching the other mid-transition rather than throwing — but give a genuine row
  a real timeout first, or a slow-rendering row gets silently dropped.
- **Judge scroll progress by every rendered row, not the topmost one.** A short list can scroll far
  enough to reveal a new row while the first row stays put; a top-only signal reads that as
  "nothing moved" and stops collecting one row early.
- **Match the scroll step to the row height.** A full-page gesture on the cart, whose rows are
  about a third of the screen, lands mid-row: the name ends up above the viewport while the
  description shows, so the row is present but unidentifiable. Both lists scroll half a page
  (`AndroidScroll.PART_PAGE`).
- **A partially rendered cart row exposes one text node, and it may be the blurb rather than the
  name.** Require both nodes before trusting "the first text node" — otherwise a description is
  silently returned where a product name was expected.
- **`CartPage.getItems()` checks itself against the cart badge** — the app's own count — and
  re-reads when short. Prefer that kind of invariant over another timing tweak: it turns a silent
  wrong answer into a diagnosable one.
- **Don't swipe the product grid.** It supports drag-to-cart, so a slow drag picks a card up.
  Use `mobile: scrollGesture`, addressed by `elementId` (a bounding box reports nothing to scroll).
- **iOS is letterboxed.** Taps land ~15pt high; `IOSTouch` measures the inset per session. See
  `docs/locator-reference.md`.
- **Colour has no attribute on native.** UiAutomator2 and XCUITest expose no style, so SL-17's
  red-border check samples screenshot pixels in `ColorUtil`. It returns `false` rather than
  throwing when a screenshot cannot be read.
- **Appium caches its driver list at startup.** A driver installed after the server started is
  invisible to it until the server restarts.
- **TestNG 7.8 holds exactly one `IAnnotationTransformer`.** `TestNG.setAnnotationTransformer`
  drops a second registration with nothing but an `"AnnotationTransformer already set"` warning, and
  Carina's `CarinaListenerChain` already is one — registered, like ours, through the service loader,
  so jar order would decide the winner. That is why retry analyzers are attached from an
  `ITestListener` (TestNG keeps a *list* of those) via `ITestNGMethod.setRetryAnalyzerClass`, and not
  by rewriting the `@Test` annotation. **Do not add an `IAnnotationTransformer` to this project**
  without checking it is the only one.
- **Allure's version choice is constrained twice, and both fail confusingly.** `allure-maven` 3.x
  is compiled to **Java 17** bytecode and simply cannot run on this project's JDK 11 — hence the pin
  to `2.18.0`. And `allure-testng` 2.29+ moves to **slf4j 2.x**, which does not match the
  `log4j-slf4j-impl` (slf4j 1.7) binding used here; `2.25.0` is the last release on slf4j 1.7.x.
  Both pins carry a comment in `pom.xml`. Do not bump either without re-checking those two things.
- **`allure-testng` drags in TestNG 6.14.3 and wins.** It declares its own `org.testng:testng`
  dependency, and as a *direct* dependency that sits nearer than Carina's 7.8.0 — so Maven silently
  downgrades TestNG for the whole project and `RetryAnalyzerListener` stops compiling on
  `org.testng.internal.annotations.DisabledRetryAnalyzer` (7.x only). `pom.xml` excludes it.
- **Report metadata must be written from `afterInvocation`, not `onTestFailure`.** Allure closes a
  test case from its own `ITestListener` callbacks, and the order of two `ITestListener`s found by
  the service loader is unspecified — labels and attachments written from `onTestFailure` are a race
  that silently loses them. TestNG runs every `IInvokedMethodListener.afterInvocation` first, which
  is deterministic at both ends.
- **`IDriverPool.getDriver()` *starts* a session when the thread has none.** Calling it from a
  reporting path means a driverless suite (`retry_demo`) tries to open an Appium session just to
  report a failure — it cost 30s per failed test before `AllureAdapterListener` was made to check
  `isDriverRegistered(...)` first.
- **Keep the reporting classes out of the step bridge.** The appender is scoped to
  `com.mobile.swaglabs.qa`, which includes the listeners themselves, so without a dedicated
  `com.mobile.swaglabs.qa.listener` logger in `log4j2.xml` a line like *"Could not attach a failure
  screenshot"* shows up in the report as a test step.
- **Carina drops a blank or `NULL` capability only at JVM start, never afterwards.** `R`'s static
  init strips blank and `NULL` `capabilities.*` entries, but `CapabilitiesLoader` and
  `R.CONFIG.put` do not, and the read side (`AbstractCapabilities.getGlobalCapabilities`) filters
  `null` alone. So a `capabilities.udid=NULL` in a properties file, or an empty
  `-Dcapabilities.app` re-applied after that init, reaches Appium as a real capability with the
  literal value. That is why device identity lives in a separate `-local` file rather than being
  blanked out, and why `applyCommandLineOverrides` skips empty values itself.
- **`CarinaListener` overwrites a suite's `thread-count`.** It rewrites `thread-count` and
  `data-provider-thread-count` from `thread_count` / `data_provider_thread_count` in
  `_config.properties`, both `1` here, so declaring them in a suite XML has no effect. A suite that
  really wants parallelism has to be run with `-Dthread_count=N` — see the `retry_demo` command.
