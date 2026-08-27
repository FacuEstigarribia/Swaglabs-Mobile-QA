# Swag Labs locator reference

Captured from live UI dumps on 2026-08-26:
Android `com.swaglabsmobileapp` on `emulator-5554`, iOS `com.saucelabs.SwagLabsMobileApp`
on the iPhone 17 simulator (iOS 26.5). Both platforms expose the **same accessibility ids**, so
`@ExtendedFindBy(accessibilityId = "...")` resolves on both without a platform-specific locator.

## Accessibility ids by screen

| Screen | Element | Accessibility id |
|---|---|---|
| Login | Root container | `test-Login` |
| Login | Username field | `test-Username` |
| Login | Password field | `test-Password` |
| Login | Login button | `test-LOGIN` |
| Login | Error banner | `test-Error message` |
| Login | Autofill shortcuts | `test-standard_user`, `test-locked_out_user`, `test-problem_user` |
| Products | Scroll container (page marker) | `test-PRODUCTS` |
| Products | Header bar | `test-Cart drop zone` |
| Products | Hamburger menu | `test-Menu` |
| Products | Cart button | `test-Cart` |
| Products | Grid/list toggle | `test-Toggle` |
| Products | Sort selector | `test-Modal Selector Button` |
| Products | Product card | `test-Item` |
| Products | Card title / price | `test-Item title`, `test-Price` |
| Products | Add / remove | `test-ADD TO CART`, `test-REMOVE` |
| Details | Page marker | `test-Inventory item page` |
| Details | Back | `test-BACK TO PRODUCTS` |
| Details | Name + description | `test-Description` |
| Cart | Page marker | `test-Cart Content` |
| Cart | Line item | `test-Item`, with `test-Amount`, `test-Description`, `test-Price`, `test-REMOVE` |
| Cart | Buttons | `test-CONTINUE SHOPPING`, `test-CHECKOUT` |
| Menu | Entries | `test-ALL ITEMS`, `test-WEBVIEW`, `test-QR CODE SCANNER`, `test-GEO LOCATION`, `test-DRAWING`, `test-ABOUT`, `test-LOGOUT`, `test-RESET APP STATE`, `test-Close` |

Sort options are plain text on both platforms: `Name (A to Z)`, `Name (Z to A)`,
`Price (low to high)`, `Price (high to low)`, `Cancel`.

## Platform differences that justify the `@DeviceType` split

| Aspect | Android | iOS |
|---|---|---|
| Accessibility attribute in XPath | `content-desc` | `name` |
| Element classes | `android.widget.TextView`, `EditText`, `ViewGroup` | `XCUIElementTypeStaticText`, `TextField`, `Other` |
| Cart badge | Child `TextView` of `test-Cart`, holding the count as text | The **`label`** of `test-Cart` itself |
| Product list | Virtualized: cards near the viewport carry their price and buttons, others do not | Also virtualized — the page source lists all 6, but only rendered cards are usable |
| Cart line items | Also virtualized: a 3-item cart reports only the 2 that are rendered | Also virtualized |
| Error banner text | Child `TextView`'s text | The banner element's own `label` |
| Sort option element | `android.widget.TextView[@text='…']` | `XCUIElementTypeOther[@name='…']` |
| Tap coordinates | 1:1 with reported bounds | Letterboxed — see below |
| Scroll mechanism | `mobile: scrollGesture`, which returns whether more content remains | `mobile: scroll`, which returns nothing — the end is detected by the top item no longer changing |

### The iOS letterbox offset

The app has no launch storyboard for the iPhone 17 screen, so iOS runs it scaled and centred.
WebDriverAgent reports the app window as **390×844** while the real screen is **402×874**, and
touches are delivered in screen space. The result is that every tap lands ~15pt too high.

Large body controls absorb the error, but the header controls (`test-Menu`, `test-Cart`, both
50pt tall at the very top) miss entirely — a plain `.click()` on the cart silently does nothing.

Verified two independent ways:
- Empirical tap sweep: the cart opens at y=60, not at its reported centre y=50.
- `mobile: deviceScreenInfo` reports `screenSize` 402×874 against a 390×844 window.

The offset is therefore computable at runtime and is applied by `IOSSwagLabsAbstractPage`:

```
offsetX = (screenWidth  - windowWidth ) / 2   =  6
offsetY = (screenHeight - windowHeight) / 2   = 15
```

It is derived per session rather than hard-coded, so a different simulator recalculates it (and
yields 0 on a device the app supports natively).

## Error state (SL-17)

Triggered by submitting `invalid_user` / `secret_sauce`.

- Banner `test-Error message` reads exactly:
  `Username and password do not match any user in this service.`
- A cross glyph (FontAwesome `times-circle`, U+F057) appears inside **both** fields.
  Android: a `TextView` in the sibling wrapper of the field.
  iOS: `XCUIElementTypeStaticText` named `iconIcon` in the sibling wrapper.
- Both field borders render as an 8pt underline in **`#E2231A`** (RGB 226, 35, 26) directly
  below the field. Verified on both platforms; the clean login screen shows 0% red in that band
  and the error state shows 100%, so the pixel check discriminates.

## Test users

The login screen documents the accepted accounts: `standard_user`, `locked_out_user`,
`problem_user` — all with password `secret_sauce`. This build does **not** include
`performance_glitch_user` or `visual_user`, which exist in the web version of Swag Labs.

## Both platforms virtualize their lists

An early reading of the page dumps suggested iOS exposed the whole catalog at once. Running the
suite disproved that: iOS collected 2 of 6 products until real scrolling was added. Treat both
platforms as virtualized — read any full list by scrolling and accumulating.

## Two ids that are not unique to one screen

`test-Item` identifies **both** a product-grid card and a cart line item. Code that collects one
must tolerate matching the other while the screen is mid-transition — a product card has no
`test-Description`, so reading it as a cart row throws.

## Taps during re-render

The app drops taps issued while a list is re-rendering, which happens right after adding to or
removing from the cart. Both `ProductsPage.addProductsToCart` and `CartPage.removeItem` therefore
confirm the app acted — the card flipping to REMOVE, or the row disappearing — and retry once
rather than assuming the tap landed.

## Reading a list reliably

Two habits make list reads deterministic on both platforms:

1. **Judge scroll progress from every rendered row**, not just the first. A short list can scroll
   enough to reveal a new row while the top row stays put, and a top-only signal stops collection
   one row early.
2. **Match the scroll step to the row height.** Cart rows are roughly a third of the screen; a
   full-page gesture lands mid-row, leaving a row whose name is above the viewport and whose
   description is visible. The cart scrolls half a page, the product grid a full one.
3. **Require a row to be fully rendered before reading it.** A partially rendered cart row exposes
   a single text node, and it can be the description rather than the name.
4. **Check the result against the cart badge.** It is the app's own count, so a read that comes up
   short is detectable and can be retried rather than quietly returning the wrong answer.
