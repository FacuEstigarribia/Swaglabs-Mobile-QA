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
# Regression - the full 22 cases
mvn clean test -Dsuite=regression       
mvn clean test -Dsuite=ios_regression   

# Smoke - three cases, enough to prove the rig works
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

`docs/test-cases-formatted.csv` is the canonical one-row-per-case source. The two supported
Zebrunner import shapes are generated rather than maintained by hand:

```bash
python3 docs/generate_zebrunner_import.py   # writes docs/zebrunner-test-cases.csv
python3 docs/test_cases_formatted_import.py # writes docs/test-cases-formatted-import.csv

# Generate an import containing only selected cases, avoiding duplicates on repeat imports.
python3 docs/generate_zebrunner_import.py --ids SL-18,SL-19,SL-20,SL-21,SL-22
```

For import targets that require one row per test case and a numbered multiline
`Step Action` field, generate the formatted import instead:

```bash
python3 docs/test_cases_formatted_import.py --ids SL-18,SL-19,SL-20,SL-21,SL-22
```

This writes `docs/test-cases-formatted-import.csv`, preserving the nested suite path from
`docs/test-cases-formatted.csv` and renaming its `Step` column to `Step Action`.

Upload the result on the project's **Test Cases** page (*Import > CSV*). The mapping it applies:

| `test-cases-formatted.csv` | Zebrunner step-per-row export |
|---|---|
| `Title`, including `SL-nn` | `Title`, repeated on each step row |
| `Suite` | `Suite`, including the nested `Swag Labs Mobile > <Area>` path |
| `Description`, `Pre-conditions`, `Priority`, `Automation State` | copied unchanged |
| numbered multiline `Step` | one unnumbered `Step` per row |
| numbered multiline `Expected Result` | the matching unnumbered result per row |

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
docs/test-cases-formatted.csv           Source of truth for the test design (one row per case)
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

## Test cases

22 cases cover the product grid, filtering, cart, checkout, account, and login.
`docs/test-cases-formatted.csv` is the source of truth. After editing it, regenerate the catalog
and both Zebrunner import formats:

```bash
python3 docs/generate_readme_cases.py
python3 docs/generate_zebrunner_import.py
python3 docs/test_cases_formatted_import.py
```

<!-- BEGIN GENERATED TEST CASES -->

### Product Grid

#### SL-01 — Product grid shows all catalog items

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `ProductGridTest.testProductGridDisplaysAllItems`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Count the product cards rendered in the grid | Exactly 6 product cards are displayed |
| 3 | Read the product name on every card | Every card shows a non-empty product name |
| 4 | Read the price label on every card | Every price matches the format $X.XX |
| 5 | Check that every card renders a product image | An image element is present and displayed on every card |
| 6 | Check that every card exposes an ADD TO CART control | An enabled ADD TO CART button is present on every card |

#### SL-02 — Grid and list view toggle changes layout and preserves items

- **Priority:** P2
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `ProductGridTest.testToggleGridAndListView`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Record the ordered list of product names in the default view | A list of 6 product names is captured |
| 3 | Tap the view toggle control | The layout switches to the alternate view and the toggle remains displayed |
| 4 | Record the ordered list of product names in the toggled view | Still exactly 6 product cards are displayed |
| 5 | Compare the two recorded name lists | Both lists contain the same 6 names in the same order |
| 6 | Tap the view toggle control again | The layout returns to the original view with the same 6 products |

#### SL-03 — Opening a product shows matching details

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `ProductGridTest.testOpenProductDetailsFromGrid`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Record the name and price of the first product card | Name and price values are captured from the grid |
| 3 | Tap the first product card | The product details page opens |
| 4 | Compare the details page name with the recorded grid name | The product name on the details page equals the name recorded from the grid |
| 5 | Compare the details page price with the recorded grid price | The price on the details page equals the price recorded from the grid |
| 6 | Inspect the description and ADD TO CART control on the details page | A non-empty description and an enabled ADD TO CART button are displayed |

#### SL-04 — Back from product details returns to an unchanged grid

- **Priority:** P2
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `ProductGridTest.testReturnFromDetailsToGrid`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Record the ordered list of product names in the grid | A list of 6 product names is captured |
| 3 | Open the second product card | The product details page opens for the selected product |
| 4 | Tap the back control on the details page | The Products page is displayed again with the PRODUCTS header |
| 5 | Record the ordered list of product names again | The list matches the list recorded in step 2 exactly |
| 6 | Check the cart badge | No cart badge count is displayed because nothing was added |

### Filtering

#### SL-05 — Sort by Name A to Z

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** Name (A to Z); standard_user / secret_sauce
- **Automated by:** `ProductFilterTest.testSortByNameAscending`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Name (A to Z) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of product names | Still exactly 6 product cards are displayed |
| 5 | Compare the list against the same names sorted ascending case-insensitively | The displayed order equals the expected ascending order |

#### SL-06 — Sort by Name Z to A

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** Name (Z to A); standard_user / secret_sauce
- **Automated by:** `ProductFilterTest.testSortByNameDescending`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Name (Z to A) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of product names | Still exactly 6 product cards are displayed |
| 5 | Compare the list against the same names sorted descending case-insensitively | The displayed order equals the expected descending order |

#### SL-07 — Sort by Price low to high

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** Price (low to high); standard_user / secret_sauce
- **Automated by:** `ProductFilterTest.testSortByPriceAscending`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Price (low to high) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of prices and parse them as numbers | Six numeric prices are parsed successfully |
| 5 | Verify each price is not greater than the next one | Prices are in non-decreasing order from first to last |

#### SL-08 — Sort by Price high to low

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** Price (high to low); standard_user / secret_sauce
- **Automated by:** `ProductFilterTest.testSortByPriceDescending`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Price (high to low) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of prices and parse them as numbers | Six numeric prices are parsed successfully |
| 5 | Verify each price is not lower than the next one | Prices are in non-increasing order from first to last |

### Cart

#### SL-09 — Add a single item from the grid updates the cart badge

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `CartTest.testAddSingleItemFromGrid`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Check the cart badge before adding anything | No cart badge count is displayed |
| 3 | Tap ADD TO CART on the first product card | The button on that card changes to REMOVE |
| 4 | Read the cart badge | The cart badge displays 1 |
| 5 | Open the cart | The cart page opens and displays exactly 1 line item |
| 6 | Compare the cart line item with the added product | The cart line item name and price match the product added in step 3 |

#### SL-10 — Add an item from the product details page

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `CartTest.testAddItemFromProductDetails`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the first product card | The product details page opens |
| 3 | Record the product name and price on the details page | Name and price values are captured |
| 4 | Tap ADD TO CART on the details page | The button changes to REMOVE and the cart badge displays 1 |
| 5 | Open the cart from the details page | The cart page opens and displays exactly 1 line item |
| 6 | Compare the cart line item with the recorded values | The cart line item name and price match the values recorded in step 3 |

#### SL-11 — Add multiple items and verify cart contents match the badge

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `CartTest.testAddMultipleItemsToCart`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Tap ADD TO CART on the first product card | The cart badge displays 1 |
| 3 | Tap ADD TO CART on the second product card | The cart badge displays 2 |
| 4 | Tap ADD TO CART on the third product card | The cart badge displays 3 |
| 5 | Open the cart | The cart page opens and displays exactly 3 line items |
| 6 | Compare the cart line item names with the three added products | The cart contains exactly the three product names added in steps 2 to 4 |

#### SL-12 — Remove an item from the cart

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `CartTest.testRemoveItemFromCart`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Add the first two product cards to the cart | The cart badge displays 2 |
| 3 | Open the cart | The cart page opens and displays exactly 2 line items |
| 4 | Tap REMOVE on the first cart line item | That line item disappears and exactly 1 line item remains |
| 5 | Read the cart badge | The cart badge displays 1 |
| 6 | Tap REMOVE on the remaining cart line item | The cart is empty and no cart badge count is displayed |

#### SL-13 — Cart contents survive Continue Shopping

- **Priority:** P2
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `CartTest.testCartPersistsAfterContinueShopping`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Add the first two product cards to the cart | The cart badge displays 2 |
| 3 | Open the cart and record the line item names | The cart page opens and 2 line item names are captured |
| 4 | Tap CONTINUE SHOPPING | The Products page is displayed again with the PRODUCTS header |
| 5 | Read the cart badge on the Products page | The cart badge still displays 2 |
| 6 | Reopen the cart and read the line item names | The cart still contains exactly the 2 names recorded in step 3 |

#### SL-18 — Successful checkout calculates totals and completes the purchase

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce; Test / User / 12345
- **Automated by:** `CartTest.testSuccessfulCheckoutCalculatesTotalsAndCompletesPurchase`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Add the first two products and record their names and displayed prices | The cart badge displays 2 and the product values are captured from the UI |
| 3 | Open the cart and tap CHECKOUT | The checkout information page opens |
| 4 | Enter valid first name last name and postal code then tap CONTINUE | The checkout overview page opens |
| 5 | Compare the overview line items with the products recorded from the UI | The same product names and displayed prices appear in the overview |
| 6 | Calculate the expected subtotal by summing the recorded UI prices | The displayed item subtotal equals the calculated subtotal |
| 7 | Calculate tax at 8 percent from the expected subtotal and add it to the subtotal | The displayed tax and total equal the values calculated from the UI prices |
| 8 | Tap FINISH | The checkout complete page opens and BACK HOME is displayed |
| 9 | Tap BACK HOME | The Products page opens and the cart badge is cleared |

#### SL-19 — Checkout requires a first name

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce; User / 12345
- **Automated by:** `CartTest.testCheckoutRequiresFirstName`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user and add the first product | The Products page opens and the cart badge displays 1 |
| 2 | Open the cart and tap CHECKOUT | The checkout information page opens |
| 3 | Leave First Name empty | The First Name field remains empty |
| 4 | Enter a valid last name and postal code | The Last Name and Zip/Postal Code fields contain the entered values |
| 5 | Tap CONTINUE | The checkout information page remains open |
| 6 | Read the error banner | The error banner displays First Name is required |

#### SL-20 — Checkout requires a last name

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce; Test / 12345
- **Automated by:** `CartTest.testCheckoutRequiresLastName`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user and add the first product | The Products page opens and the cart badge displays 1 |
| 2 | Open the cart and tap CHECKOUT | The checkout information page opens |
| 3 | Enter a valid first name and postal code | The First Name and Zip/Postal Code fields contain the entered values |
| 4 | Leave Last Name empty | The Last Name field remains empty |
| 5 | Tap CONTINUE | The checkout information page remains open |
| 6 | Read the error banner | The error banner displays Last Name is required |

#### SL-21 — Checkout requires a postal code

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce; Test / User
- **Automated by:** `CartTest.testCheckoutRequiresPostalCode`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user and add the first product | The Products page opens and the cart badge displays 1 |
| 2 | Open the cart and tap CHECKOUT | The checkout information page opens |
| 3 | Enter a valid first name and last name | The First Name and Last Name fields contain the entered values |
| 4 | Leave Zip/Postal Code empty | The Zip/Postal Code field remains empty |
| 5 | Tap CONTINUE | The checkout information page remains open |
| 6 | Read the error banner | The error banner displays Postal Code is required |

#### SL-22 — Cancelling checkout preserves the cart

- **Priority:** P2
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce; Test / User / 12345
- **Automated by:** `CartTest.testCheckoutCancelPreservesCart`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user and add the first product while recording its UI values | The Products page opens and the cart badge displays 1 |
| 2 | Open the cart and tap CHECKOUT | The checkout information page opens |
| 3 | Tap CANCEL on checkout information | The Products page opens and the cart badge still displays 1 |
| 4 | Reopen the cart and compare its item with the recorded UI values | The cart still contains the same product name and displayed price |
| 5 | Return to checkout and submit valid customer information | The checkout overview page opens |
| 6 | Tap CANCEL on checkout overview | The Products page opens and the cart badge still displays 1 |
| 7 | Reopen the cart and compare its item with the recorded UI values | The cart still contains the same product name and displayed price |

### Account

#### SL-14 — Menu exposes all expected navigation items

- **Priority:** P2
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `AccountTest.testMenuItemsAreDisplayed`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Tap the hamburger menu control | The navigation menu opens |
| 3 | Check for the ALL ITEMS entry | The ALL ITEMS entry is displayed |
| 4 | Check for the WEBVIEW and QR CODE SCANNER entries | Both entries are displayed |
| 5 | Check for the GEO LOCATION and DRAWING entries | Both entries are displayed |
| 6 | Check for the ABOUT RESET APP STATE and LOGOUT entries | All three entries are displayed |
| 7 | Close the menu | The menu closes and the Products page is displayed again |

#### SL-15 — Logout returns to the login screen with fields cleared

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `AccountTest.testLogoutReturnsToLoginPage`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Tap the hamburger menu control | The navigation menu opens and the LOGOUT entry is displayed |
| 3 | Tap LOGOUT | The login page opens with the username and password fields displayed |
| 4 | Read the username field value | The username field is empty |
| 5 | Read the password field value | The password field is empty |
| 6 | Check that no error message is shown on the login page | No login error banner is displayed |

### Login

#### SL-16 — Every valid user in the pool can log in and reach Products

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / problem_user
- **Automated by:** `LoginValidationTest.testLoginWithAllValidUsers`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with the valid user supplied by the data provider | Products page opens and the PRODUCTS header is displayed |
| 2 | Verify the product grid rendered for this user | Exactly 6 product cards are displayed |
| 3 | Check that no login error banner is present | No login error banner is displayed |
| 4 | Open the menu and tap LOGOUT | The login page opens with empty username and password fields |
| 5 | Repeat steps 1 to 4 for every remaining user in the valid pool | Every valid user in the pool (standard_user, problem_user) logs in successfully and logs out cleanly |

#### SL-17 — Invalid username is rejected with field and banner errors

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** invalid_user / secret_sauce
- **Automated by:** `LoginValidationTest.testLoginWithInvalidUsernameIsRejected`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Open the app and confirm the login page is displayed | The login page opens with username password and LOGIN controls displayed |
| 2 | Enter an invalid username and a valid password then tap LOGIN | The login page remains displayed and no navigation occurs |
| 3 | Check whether the Products page opened | The Products page is NOT open |
| 4 | Check the error banner on the login page | The error banner is displayed with the text: Username and password do not match any user in this service. |
| 5 | Inspect the username field | A cross (X) error icon is displayed inside the username field |
| 6 | Inspect the password field | A cross (X) error icon is displayed inside the password field |
| 7 | Inspect the border colour of the username field | The username field border is rendered red |
| 8 | Inspect the border colour of the password field | The password field border is rendered red |

<!-- END GENERATED TEST CASES -->
