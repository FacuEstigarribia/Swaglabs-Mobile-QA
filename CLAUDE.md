# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# QAMobileSwaglabs — working notes

Carina 1.3.0 + Appium UI automation for the Swag Labs mobile app, running one suite against Android
and iOS. JDK 11, TestNG, no unit tests — every test drives
a real device or simulator through a local Appium server at `http://localhost:4723`.

## Commands

```bash
mvn clean test -Dsuite=android      # all 17 cases, Android
mvn clean test -Dsuite=ios          # all 17 cases, iOS
mvn clean test -Dsuite=smoke        # SL-01 + SL-17 on Android — checks the rig
mvn clean test -Dsuite=ios_smoke    # the same two on iOS
mvn clean test -Dsuite=cart         # CartTest on Android
mvn clean test -Dsuite=ios_cart     # CartTest on iOS
python3 docs/generate_readme_cases.py   # after editing docs/test-cases.csv
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

Artifacts: `target/screenshots/` (failure PNGs), `target/logs/test.log`, `target/reports/`.

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
  `capabilities.*` out of TestNG suite parameters, which is why the indirection exists — and because
  that load happens after JVM start, the method then re-applies any `-Dcapabilities.*` so a
  command-line override still wins. `_config.properties` stays platform-neutral.
- **Driver lifecycle.** `driver_mode=method_mode`, so Carina relaunches the app per test *method*:
  every test starts with an empty cart and no session, which is what makes rule 1 cheap. It is
  *not* per data-provider invocation — SL-16 logs out explicitly between users for that reason.
- **Users.** `_testdata.properties` declares pools (`::`-separated member keys, each resolving to
  `<key>.login` / `<key>.password`). The `UserPool` enum implements `UserProvider`, so a pool
  constant goes straight into `getLoginService().login(VALID_USERS_POOL)`. `UsersPool.getUser()`
  leases per thread and returns the *same* user on a repeat call for that pool;
  `getAllUsers()` reads a pool without leasing it, for data providers; `SwagLabsBaseTest`
  releases in `@AfterMethod`.
- **Suites list classes explicitly.** A new test class must be added to both `android.xml` and
  `ios.xml`, and the `ScreenshotOnFailureListener` is registered per suite XML too.
- **Test metadata.** Every `@Test` carries `@MethodOwner`, `@TestPriority`, and
  `@TestTag(tcId=SL-nn)` + `@TestTag(feature=…)`. `docs/test-cases.csv` is the source of truth for
  the case design; the README's case section is generated from it.

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
