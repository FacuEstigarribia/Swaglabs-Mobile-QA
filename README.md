# QA Mobile — Swag Labs (Carina + Appium)

Cross-platform UI test automation for the Sauce Labs **Swag Labs** demo app, running the same
test suite against **Android** and **iOS**.

Built on [Carina](https://github.com/zebrunner/carina) and following the architectural
conventions: a layered `Tests → Services → Pages → Components` structure,
a properties-backed user pool, and hard assertions on every page transition.

---

## Prerequisites

| Requirement | Version used |
|---|---|
| JDK | 11 |
| Maven | 3.9+ |
| Appium server | 3.x, listening on `http://localhost:4723` |
| Appium drivers | `uiautomator2` (Android), `xcuitest` (iOS) |
| Android | An emulator or device with `com.swaglabsmobileapp` installed |
| iOS | A simulator with `com.saucelabs.SwagLabsMobileApp` installed |

The app must already be installed on the target device — the suites launch it by
package/bundle id rather than installing an artifact.

Start Appium before running anything:

```bash
appium
```

> **If the iOS suite fails with `Could not find a driver for automationName 'XCUITest'`,**
> restart the Appium server. It caches its driver list at startup, so a driver installed after
> the server was launched stays invisible to it. `appium driver list --installed` will show the
> driver as present while the running server still cannot see it.

The first iOS run builds WebDriverAgent, which takes several minutes; later runs reuse it.

> **If a run starts fine and then every later test fails** with `socket hang up`,
> `A session is either terminated or not started`, or `the instrumentation process is not
> running`, the UiAutomator2 server on the device has crashed — it does not recover on its own.
> Reset it and the next session reinstalls it:
>
> ```bash
> adb uninstall io.appium.uiautomator2.server
> adb uninstall io.appium.uiautomator2.server.test
> ```
>
> Killing a run mid-flight makes this more likely, because the Appium session is never closed.
> Stop a run with `pkill -f ForkedBooter` (the surefire JVM is named `ForkedBooter`, so
> `pkill -f surefire` does *not* match it) and give orphaned sessions time to expire.

## Running the tests

Suites live in `src/test/resources/testng_suites/` and are selected with `-Dsuite=`.

```bash
# Regression - the full 17 cases
mvn clean test -Dsuite=regression       
mvn clean test -Dsuite=ios_regression   

# Smoke - two cases, enough to prove the rig works
mvn clean test -Dsuite=smoke
mvn clean test -Dsuite=ios_smoke

# Features - one area at a time, for iterating without a full-suite cycle
mvn clean test -Dsuite=grid      
mvn clean test -Dsuite=filter    
mvn clean test -Dsuite=cart       
mvn clean test -Dsuite=account    
mvn clean test -Dsuite=login      
```

Every feature suite has an iOS twin: `ios_grid`, `ios_filter`, `ios_cart`, `ios_account`,
`ios_login`.

Each suite names a `platform`, which selects the matching
`src/main/resources/capabilities/<platform>.properties`. Carina does not read `capabilities.*`
from TestNG suite parameters, so `SwagLabsBaseTest` loads that file in `@BeforeSuite`.

That file holds only capabilities that are true anywhere. Which device to drive comes from
`capabilities/<platform>-local.properties` — `deviceName` for Android, plus `udid` and
`platformVersion` for iOS — and it is applied only when no device farm is serving the run. Point the
suite at a different emulator or simulator by editing that file once, rather than passing `-D` flags
every time.

Any capability can be overridden on the command line, which wins over the file:

```bash
mvn clean test -Dsuite=android -Dcapabilities.deviceName=emulator-5556
mvn clean test -Dsuite=ios -Dcapabilities.udid=<YOUR-SIMULATOR-UDID>
```

## Reports

The suite produces an [Allure](https://allurereport.org/) report. Nothing has to be installed:
the Maven plugin fetches the Allure commandline itself on first use.

```bash
mvn clean test -Dsuite=regression   # writes target/allure-results/
mvn allure:serve                    # builds the report and opens it in a browser
mvn allure:report                   # or: static HTML in target/allure-report/
```

What the report gives you, all of it derived from metadata the tests already declare:

| Tab | Shows |
|---|---|
| **Behaviors** | Cases grouped by feature - Product Grid, Filtering, Cart, Account, Login - read from `@TestTag(name = "feature")`. This is the view to open after a regression run. |
| **Suites** | Grouped by platform, so an Android and an iOS run stay distinguishable side by side. |
| **Categories** | Failures bucketed by cause. *Driver / session lost* separates a dropped UiAutomator2 instrumentation from a genuine *Assertion failed* - the two look identical in a flat list. |
| **Retries** | Every attempt when a test is re-run with `-Dretry_count=N`. |

Each test shows its owner, a severity taken from `@TestPriority`, its `SL-nn` tag with a link to the
matching case in `docs/test-cases.md`, and a step list built from the log lines the pages and
services already emit (`Adding 'Sauce Labs Backpack' to the cart.`). A failing test carries a
screenshot of the screen as it was when it failed.

The Environment panel records which device, Appium server and app build the run actually used, plus
the effective `retry_count`.

Failure screenshots are also written to `target/screenshots/` and to Carina's report directory,
independently of Allure.

### Retrying failed tests

A failed test method is re-run up to `retry_count` times. `retry_count` is `0` in
`_config.properties`, so nothing is retried until you ask for it:

```bash
mvn clean test -Dsuite=android -Dretry_count=2   
```

The count is resolved most-specific-first: `-Dretry_count=N`, then a
`<parameter name="retry_count" value="N"/>` in the suite or `<test>` XML, then `_config.properties`.

No test declares `@Test(retryAnalyzer = …)`. `RetryAnalyzerListener` attaches
`RetryCountAnalyzer` to every method it finds, and TestNG discovers that listener through
`src/main/resources/META-INF/services/org.testng.ITestNGListener` — so the mechanism covers every
suite, including ones added later, with no annotation and no suite edit. While `retry_count` is `0`
the listener attaches nothing at all.

Attempts are counted per *invocation* — class, method, and parameters — in a concurrent map, so each
data-provider row keeps its own budget and parallel threads cannot corrupt each other's count.
Because `driver_mode=method_mode`, a retried mobile test gets a freshly launched app, and
`SwagLabsBaseTest` releases and re-leases its user, so the retry is genuinely independent.

`src/test/resources/testng_suites/retry_demo.xml` demonstrates the logic without a device:

```bash
mvn clean test -Dsuite=retry_demo -Dthread_count=4 -Ddata_provider_thread_count=3
```

`RetryAnalyzerDemoTest` holds primitive tests that fail on purpose — one that passes on its retry,
one that never passes, one that passes first time, and a three-row data provider whose rows each
recover independently. **The suite is meant to end red:** the always-failing case is what proves the
budget runs out. The two `-D` flags are needed because `CarinaListener` rewrites a suite's
`thread-count` from `thread_count`, which is `1` for the device suites.

## Zebrunner

The suite reports to [Zebrunner](https://solvdinternal.zebrunner.com/projects/SAUCEM) and its 17
cases live there as test cases. No extra dependency is involved: `carina-core` already brings
`com.zebrunner:agent-core`, so this is configuration only.

### Reporting a run

`src/main/resources/agent.properties` carries the hostname and `reporting.project-key=SAUCEM`, and
leaves reporting **off** so a local run stays local. The access token is a credential and is not in
the repository:

```bash
export REPORTING_SERVER_ACCESS_TOKEN=<token from Zebrunner > Account & Profile>
mvn clean test -Dsuite=smoke -Dreporting.enabled=true
```

### Test cases

`docs/test-cases.csv` stays the source of truth. The importer takes a different shape, so it is
generated rather than maintained by hand:

```bash
python3 docs/generate_zebrunner_import.py   # writes docs/zebrunner-test-cases.csv
```

Upload the result on the project's **Test Cases** page (*Import > CSV*). The mapping it applies:

| `test-cases.csv` | Zebrunner |
|---|---|
| `Title`, prefixed with `TC_ID` | `Title` — Zebrunner assigns its own `SAUCEM-nn` keys, so `SL-nn` is kept in the title to stay searchable |
| `Area` | `Suite`, nested as `Swag Labs Mobile > <Area>` (missing suites are created on import) |
| `Precondition` | `Pre-conditions` |
| `Priority` | `Priority` — `P1`/`P2` verbatim; unknown values are created on import |
| `Action` (+ `Test Data` inlined) | `Step`, one per row |
| `Expected Result` | `Expected Result` |
| `Platform`, `Automated Method`, `TC_ID` | folded into `Description` — the importer ignores columns it does not recognise |

Once the cases exist, add `@TestCaseKey("SAUCEM-nn")` to each `@Test` so results land against them.

### Launchers

Register the repository on Zebrunner's **Launchers** page. The scan generates one job per TestNG
suite XML it finds under `src/test/resources/testng_suites/`, and reads the target project from
`reporting.project-key` in `agent.properties`.

Each suite carries the metadata that shapes its launcher — `jenkinsJobName`, `jenkinsJobType`
(`android` or `ios`), `provider`, `jenkinsMobileDefaultPool`, `suiteOwner`, `jenkinsEmail`,
`jenkinsEnvironments`, and a `capabilities.app` field for the build to install. Carina itself never
reads any of them, so they change nothing about a local run.

| Suite | Launcher |
|---|---|
| `android.xml` / `ios.xml` | `Swaglabs-Android-Full` / `Swaglabs-iOS-Full` |
| `regression.xml` / `ios_regression.xml` | `Swaglabs-Android-Regression` / `Swaglabs-iOS-Regression` |
| `smoke.xml` / `ios_smoke.xml` | `Swaglabs-Android-Smoke` / `Swaglabs-iOS-Smoke` |
| `grid`, `filter`, `cart`, `account`, `login` (+ `ios_` twins) | `Swaglabs-<Platform>-<Area>` |
| `retry_demo.xml` | `Swaglabs-Retry-Demo` — **run this one first**: it drives no device, so it proves the pipeline without a device farm |

`Swaglabs-Retry-Demo` is the one launcher that needs no device farm at all — it is a driverless
suite (`jenkinsJobType=api`), so it is the cheapest way to confirm that the repository scan, job
generation, reporting and retry tracking all work. It is **meant to end red**: two of its cases fail
on purpose, and they show as failed tests rather than a broken build because the pipeline hands the
job status to Zebrunner when reporting is on.

For the device suites, a launcher run differs from a local one in exactly two ways, both handled: the hub comes from the
farm (`-Dselenium_url`) instead of `localhost:4723`, and the device is leased from a pool, so
`capabilities/<platform>-local.properties` is skipped — that is what the suite's `provider`
parameter signals.

## Project structure

```
docs/test-cases.csv                     Source of truth for the test design (one row per step)
docs/locator-reference.md               Accessibility ids and the platform differences, from live dumps
docs/generate_readme_cases.py           Regenerates this file's test-case section from the CSV
docs/generate_zebrunner_import.py       Reshapes the CSV for Zebrunner's test-case importer
docs/zebrunner-test-cases.csv           Generated - the file to upload to Zebrunner TCM
src/main/java/com/mobile/swaglabs/qa/
├── IConstants.java                     Timeouts, numeric constants, assertion messages
├── SwagLabsBaseTest.java               Base test: getLoginService(), user release
├── data/                               UserData POJO, UserProvider interface
├── enums/                              UserPool, SortOption, MenuItem
├── service/                            UsersPool (lease/release), LoginService
├── pages/common/                       Abstract page objects — shared accessibility-id locators
├── pages/android/                      @DeviceType(ANDROID_PHONE) implementations
├── pages/ios/                          @DeviceType(IOS_PHONE) implementations
├── components/common/                  ProductCard (AbstractUIObject)
├── util/                               ColorUtil — pixel sampling for colour assertions
├── listener/                           ScreenshotOnFailureListener
└── retry/                              RetryCountAnalyzer + the listener that attaches it
src/main/resources/                     _config.properties, _testdata.properties, log4j2.xml,
                                        agent.properties (Zebrunner reporting)
src/main/resources/capabilities/        android.properties, ios.properties (+ the -local overlays)
src/main/resources/META-INF/services/   TestNG listener registration for the retry logic
src/test/java/.../test/                 The five test classes
src/test/java/.../retry/                RetryAnalyzerDemoTest — deliberate failures, no device
src/test/resources/testng_suites/       android.xml, ios.xml, smoke.xml, ios_smoke.xml, cart.xml,
                                        ios_cart.xml, retry_demo.xml
```

### Layering

`Tests → Services → Pages → Components`. Tests contain steps and assertions only; services own
multi-page workflows (`LoginService.login`); pages own UI interaction with no business logic;
components model repeated UI blocks such as a product card.

### Page objects across platforms

Swag Labs is a React Native app, so most accessibility ids are identical on both platforms.
Each screen therefore has **one abstract base class** in `pages/common/` holding the shared
locators, plus thin `pages/android/` and `pages/ios/` subclasses annotated with
`@DeviceType(pageType = …, parentClass = …)` that override only what genuinely differs — most
notably the sort control, which is a tappable list on Android and a native picker wheel on iOS.

Tests always declare the abstract type (`ProductsPage`), and Carina's `initPage(...)` resolves
the correct platform implementation at runtime.

The differences that actually required the split — captured from live UI dumps on both devices —
are listed in [`docs/locator-reference.md`](docs/locator-reference.md), along with every
accessibility id. The three that shape the most code:

- **The cart badge.** Android nests the count in a child text node of the cart button; iOS
  publishes it as the button's own accessibility label.
- **List virtualization.** Android keeps only on-screen cards in the view hierarchy, so reading
  the full catalog means scrolling and accumulating; iOS exposes all six at once.
- **The iOS letterbox offset.** The app ships no launch storyboard for current iPhone screens, so
  iOS renders it scaled and centred: WebDriverAgent reports a 390x844 window inside a 402x874
  screen, and every tap lands ~15pt high. Large controls absorb it, but the 50pt header buttons do
  not — a plain `click()` on the cart silently does nothing. `IOSTouch` measures the inset per
  session and taps through it, so it recomputes for a different simulator instead of relying on a
  hard-coded number.

## Test users

Users are defined in `src/main/resources/_testdata.properties` and grouped into pools:

| Pool | Members | Used by |
|---|---|---|
| `valid_users_pool` | `standard_user` | SL-01 … SL-15 |
| `all_valid_users_pool` | `standard_user`, `problem_user` | SL-16 |
| `invalid_users_pool` | `invalid_user` | SL-17 |

This app build accepts exactly three accounts, as documented on its own login screen:
`standard_user`, `locked_out_user` and `problem_user`. (`performance_glitch_user` and `visual_user`
exist only in the *web* version of Swag Labs and are not present here.)

`valid_users_pool` deliberately holds only `standard_user`, because `problem_user` renders broken
product images and would make the grid assertions flaky if the lease handed it out at random.
SL-16 is the case that intentionally exercises every account that can reach the Products page;
`locked_out_user` is excluded from it because its credentials are valid but the account is blocked.

To add a user, append its key to the relevant pool with the `::` separator and add the matching
`<key>.login` / `<key>.password` entries.

## Conventions

**Every test method logs in independently.** There is no shared login state between tests.

**Page opening is always a hard assertion.** Whenever a page opens, it is verified with
`Assert.assertTrue`, never softened:

```java
ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);
```

**`SoftAssert` groups only non-navigational assertions** — content checks that should all be
reported together rather than failing at the first one. Every `SoftAssert` ends in `assertAll()`.

> **The one exception:** **SL-17** asserts that the Products page did **not** open
> (`Assert.assertFalse(productsPage.isPageOpened(), …)`). That inversion is the entire point of
> the case. It is marked with a comment in the source — please do not "normalise" it.

**Assertion messages** live as constants in `IConstants` when reused, end with `!`, and name the
subject being asserted.