# QA Mobile — Swag Labs (Carina + Appium)

Cross-platform UI test automation for the Sauce Labs **Swag Labs** demo app, running the same
test suite against **Android** and **iOS**.

Built on [Carina](https://github.com/zebrunner/carina) and following the architectural
conventions of the `mfp-qa` project: a layered `Tests → Services → Pages → Components` structure,
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
mvn clean test -Dsuite=android   # all 17 cases on Android
mvn clean test -Dsuite=ios       # all 17 cases on iOS
mvn clean test -Dsuite=smoke     # two Android cases, for checking the rig
mvn clean test -Dsuite=ios_smoke # the same two on iOS
mvn clean test -Dsuite=cart      # the five cart cases only
```

The smoke suites are deliberately chosen: between them, SL-01 and SL-17 exercise scroll-and-collect
through the component layer, the cart badge, the whole login error state, and — on iOS — the
letterbox tap offset. If those two pass, the platform plumbing is sound.

Each suite names a `platform`, which selects the matching
`src/main/resources/capabilities/<platform>.properties`. Carina does not read `capabilities.*`
from TestNG suite parameters, so `SwagLabsBaseTest` loads that file in `@BeforeSuite`.

Any capability can be overridden on the command line, which wins over the file:

```bash
mvn clean test -Dsuite=android -Dcapabilities.deviceName=emulator-5556
mvn clean test -Dsuite=ios -Dcapabilities.udid=<YOUR-SIMULATOR-UDID>
```

Failure screenshots are written to `target/screenshots/` and to Carina's report directory.

## Project structure

```
docs/test-cases.csv                     Source of truth for the test design (one row per step)
docs/locator-reference.md               Accessibility ids and the platform differences, from live dumps
docs/generate_readme_cases.py           Regenerates this file's test-case section from the CSV
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
└── listener/                           ScreenshotOnFailureListener
src/main/resources/                     _config.properties, _testdata.properties, log4j2.xml
src/main/resources/capabilities/        android.properties, ios.properties
src/test/java/.../test/                 The five test classes
src/test/resources/testng_suites/       android.xml, ios.xml, smoke.xml, ios_smoke.xml, cart.xml
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

17 cases covering the product grid, filtering, cart, account, and login.
`docs/test-cases.csv` is the source of truth — one row per step, every step with its own
expected result. After editing it, regenerate the section below:

```bash
python3 docs/generate_readme_cases.py
```

| ID | Area | Title | Automated by |
|---|---|---|---|
| SL-01 | Product Grid | Product grid shows all catalog items | `testProductGridDisplaysAllItems` |
| SL-02 | Product Grid | Grid and list view toggle changes layout and preserves items | `testToggleGridAndListView` |
| SL-03 | Product Grid | Opening a product shows matching details | `testOpenProductDetailsFromGrid` |
| SL-04 | Product Grid | Back from product details returns to an unchanged grid | `testReturnFromDetailsToGrid` |
| SL-05 | Filtering | Sort by Name A to Z | `testSortByNameAscending` |
| SL-06 | Filtering | Sort by Name Z to A | `testSortByNameDescending` |
| SL-07 | Filtering | Sort by Price low to high | `testSortByPriceAscending` |
| SL-08 | Filtering | Sort by Price high to low | `testSortByPriceDescending` |
| SL-09 | Cart | Add a single item from the grid updates the cart badge | `testAddSingleItemFromGrid` |
| SL-10 | Cart | Add an item from the product details page | `testAddItemFromProductDetails` |
| SL-11 | Cart | Add multiple items and verify cart contents match the badge | `testAddMultipleItemsToCart` |
| SL-12 | Cart | Remove an item from the cart | `testRemoveItemFromCart` |
| SL-13 | Cart | Cart contents survive Continue Shopping | `testCartPersistsAfterContinueShopping` |
| SL-14 | Account | Menu exposes all expected navigation items | `testMenuItemsAreDisplayed` |
| SL-15 | Account | Logout returns to the login screen with fields cleared | `testLogoutReturnsToLoginPage` |
| SL-16 | Login | Every valid user in the pool can log in and reach Products | `testLoginWithAllValidUsers` |
| SL-17 | Login | Invalid username is rejected with field and banner errors | `testLoginWithInvalidUsernameIsRejected` |

<!-- BEGIN GENERATED TEST CASES -->

### Product Grid

#### SL-01 — Product grid shows all catalog items

- **Priority:** P1
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `testProductGridDisplaysAllItems`

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
- **Automated by:** `testToggleGridAndListView`

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
- **Automated by:** `testOpenProductDetailsFromGrid`

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
- **Automated by:** `testReturnFromDetailsToGrid`

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
- **Test data:** Name (A to Z), standard_user / secret_sauce
- **Automated by:** `testSortByNameAscending`

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
- **Test data:** Name (Z to A), standard_user / secret_sauce
- **Automated by:** `testSortByNameDescending`

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
- **Test data:** Price (low to high), standard_user / secret_sauce
- **Automated by:** `testSortByPriceAscending`

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
- **Test data:** Price (high to low), standard_user / secret_sauce
- **Automated by:** `testSortByPriceDescending`

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
- **Automated by:** `testAddSingleItemFromGrid`

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
- **Automated by:** `testAddItemFromProductDetails`

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
- **Automated by:** `testAddMultipleItemsToCart`

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
- **Automated by:** `testRemoveItemFromCart`

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
- **Automated by:** `testCartPersistsAfterContinueShopping`

| # | Action | Expected Result |
|---|--------|-----------------|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Add the first two product cards to the cart | The cart badge displays 2 |
| 3 | Open the cart and record the line item names | The cart page opens and 2 line item names are captured |
| 4 | Tap CONTINUE SHOPPING | The Products page is displayed again with the PRODUCTS header |
| 5 | Read the cart badge on the Products page | The cart badge still displays 2 |
| 6 | Reopen the cart and read the line item names | The cart still contains exactly the 2 names recorded in step 3 |

### Account

#### SL-14 — Menu exposes all expected navigation items

- **Priority:** P2
- **Precondition:** Swag Labs app is installed and launched on the device
- **Platforms:** Android, iOS
- **Test data:** standard_user / secret_sauce
- **Automated by:** `testMenuItemsAreDisplayed`

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
- **Automated by:** `testLogoutReturnsToLoginPage`

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
- **Automated by:** `testLoginWithAllValidUsers`

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
- **Automated by:** `testLoginWithInvalidUsernameIsRejected`

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
