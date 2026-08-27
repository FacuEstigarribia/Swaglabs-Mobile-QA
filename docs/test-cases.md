# Swag Labs mobile — test cases

17 manual test cases (13 P1, 4 P2) covering the Swag Labs mobile app, all of
them automated in this repository. Generated from [`docs/test-cases.csv`](test-cases.csv), which is the source of truth — edit the CSV
rather than this file.

Every case runs on **Android, iOS** and shares one precondition: *Swag Labs app is installed and launched on the device*.

## Summary

| ID | Area | Priority | Title | Automated by |
|---|---|---|---|---|
| [SL-01](#sl-01) | Product Grid | P1 | Product grid shows all catalog items | `testProductGridDisplaysAllItems` |
| [SL-02](#sl-02) | Product Grid | P2 | Grid and list view toggle changes layout and preserves items | `testToggleGridAndListView` |
| [SL-03](#sl-03) | Product Grid | P1 | Opening a product shows matching details | `testOpenProductDetailsFromGrid` |
| [SL-04](#sl-04) | Product Grid | P2 | Back from product details returns to an unchanged grid | `testReturnFromDetailsToGrid` |
| [SL-05](#sl-05) | Filtering | P1 | Sort by Name A to Z | `testSortByNameAscending` |
| [SL-06](#sl-06) | Filtering | P1 | Sort by Name Z to A | `testSortByNameDescending` |
| [SL-07](#sl-07) | Filtering | P1 | Sort by Price low to high | `testSortByPriceAscending` |
| [SL-08](#sl-08) | Filtering | P1 | Sort by Price high to low | `testSortByPriceDescending` |
| [SL-09](#sl-09) | Cart | P1 | Add a single item from the grid updates the cart badge | `testAddSingleItemFromGrid` |
| [SL-10](#sl-10) | Cart | P1 | Add an item from the product details page | `testAddItemFromProductDetails` |
| [SL-11](#sl-11) | Cart | P1 | Add multiple items and verify cart contents match the badge | `testAddMultipleItemsToCart` |
| [SL-12](#sl-12) | Cart | P1 | Remove an item from the cart | `testRemoveItemFromCart` |
| [SL-13](#sl-13) | Cart | P2 | Cart contents survive Continue Shopping | `testCartPersistsAfterContinueShopping` |
| [SL-14](#sl-14) | Account | P2 | Menu exposes all expected navigation items | `testMenuItemsAreDisplayed` |
| [SL-15](#sl-15) | Account | P1 | Logout returns to the login screen with fields cleared | `testLogoutReturnsToLoginPage` |
| [SL-16](#sl-16) | Login | P1 | Every valid user in the pool can log in and reach Products | `testLoginWithAllValidUsers` |
| [SL-17](#sl-17) | Login | P1 | Invalid username is rejected with field and banner errors | `testLoginWithInvalidUsernameIsRejected` |

## Product Grid

### SL-01 — Product grid shows all catalog items

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testProductGridDisplaysAllItems`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Count the product cards rendered in the grid | Exactly 6 product cards are displayed |
| 3 | Read the product name on every card | Every card shows a non-empty product name |
| 4 | Read the price label on every card | Every price matches the format $X.XX |
| 5 | Check that every card renders a product image | An image element is present and displayed on every card |
| 6 | Check that every card exposes an ADD TO CART control | An enabled ADD TO CART button is present on every card |

### SL-02 — Grid and list view toggle changes layout and preserves items

**Priority:** P2
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testToggleGridAndListView`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Record the ordered list of product names in the default view | A list of 6 product names is captured |
| 3 | Tap the view toggle control | The layout switches to the alternate view and the toggle remains displayed |
| 4 | Record the ordered list of product names in the toggled view | Still exactly 6 product cards are displayed |
| 5 | Compare the two recorded name lists | Both lists contain the same 6 names in the same order |
| 6 | Tap the view toggle control again | The layout returns to the original view with the same 6 products |

### SL-03 — Opening a product shows matching details

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testOpenProductDetailsFromGrid`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Record the name and price of the first product card | Name and price values are captured from the grid |
| 3 | Tap the first product card | The product details page opens |
| 4 | Compare the details page name with the recorded grid name | The product name on the details page equals the name recorded from the grid |
| 5 | Compare the details page price with the recorded grid price | The price on the details page equals the price recorded from the grid |
| 6 | Inspect the description and ADD TO CART control on the details page | A non-empty description and an enabled ADD TO CART button are displayed |

### SL-04 — Back from product details returns to an unchanged grid

**Priority:** P2
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testReturnFromDetailsToGrid`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Record the ordered list of product names in the grid | A list of 6 product names is captured |
| 3 | Open the second product card | The product details page opens for the selected product |
| 4 | Tap the back control on the details page | The Products page is displayed again with the PRODUCTS header |
| 5 | Record the ordered list of product names again | The list matches the list recorded in step 2 exactly |
| 6 | Check the cart badge | No cart badge count is displayed because nothing was added |

## Filtering

### SL-05 — Sort by Name A to Z

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** Name (A to Z), standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testSortByNameAscending`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Name (A to Z) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of product names | Still exactly 6 product cards are displayed |
| 5 | Compare the list against the same names sorted ascending case-insensitively | The displayed order equals the expected ascending order |

### SL-06 — Sort by Name Z to A

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** Name (Z to A), standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testSortByNameDescending`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Name (Z to A) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of product names | Still exactly 6 product cards are displayed |
| 5 | Compare the list against the same names sorted descending case-insensitively | The displayed order equals the expected descending order |

### SL-07 — Sort by Price low to high

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** Price (low to high), standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testSortByPriceAscending`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Price (low to high) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of prices and parse them as numbers | Six numeric prices are parsed successfully |
| 5 | Verify each price is not greater than the next one | Prices are in non-decreasing order from first to last |

### SL-08 — Sort by Price high to low

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** Price (high to low), standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testSortByPriceDescending`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the sort selector | The sort options list is displayed with all four sort options |
| 3 | Select Price (high to low) | The sort selector closes and the Products page is displayed |
| 4 | Read the ordered list of prices and parse them as numbers | Six numeric prices are parsed successfully |
| 5 | Verify each price is not lower than the next one | Prices are in non-increasing order from first to last |

## Cart

### SL-09 — Add a single item from the grid updates the cart badge

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testAddSingleItemFromGrid`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Check the cart badge before adding anything | No cart badge count is displayed |
| 3 | Tap ADD TO CART on the first product card | The button on that card changes to REMOVE |
| 4 | Read the cart badge | The cart badge displays 1 |
| 5 | Open the cart | The cart page opens and displays exactly 1 line item |
| 6 | Compare the cart line item with the added product | The cart line item name and price match the product added in step 3 |

### SL-10 — Add an item from the product details page

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testAddItemFromProductDetails`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Open the first product card | The product details page opens |
| 3 | Record the product name and price on the details page | Name and price values are captured |
| 4 | Tap ADD TO CART on the details page | The button changes to REMOVE and the cart badge displays 1 |
| 5 | Open the cart from the details page | The cart page opens and displays exactly 1 line item |
| 6 | Compare the cart line item with the recorded values | The cart line item name and price match the values recorded in step 3 |

### SL-11 — Add multiple items and verify cart contents match the badge

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testAddMultipleItemsToCart`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Tap ADD TO CART on the first product card | The cart badge displays 1 |
| 3 | Tap ADD TO CART on the second product card | The cart badge displays 2 |
| 4 | Tap ADD TO CART on the third product card | The cart badge displays 3 |
| 5 | Open the cart | The cart page opens and displays exactly 3 line items |
| 6 | Compare the cart line item names with the three added products | The cart contains exactly the three product names added in steps 2 to 4 |

### SL-12 — Remove an item from the cart

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testRemoveItemFromCart`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Add the first two product cards to the cart | The cart badge displays 2 |
| 3 | Open the cart | The cart page opens and displays exactly 2 line items |
| 4 | Tap REMOVE on the first cart line item | That line item disappears and exactly 1 line item remains |
| 5 | Read the cart badge | The cart badge displays 1 |
| 6 | Tap REMOVE on the remaining cart line item | The cart is empty and no cart badge count is displayed |

### SL-13 — Cart contents survive Continue Shopping

**Priority:** P2
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testCartPersistsAfterContinueShopping`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Add the first two product cards to the cart | The cart badge displays 2 |
| 3 | Open the cart and record the line item names | The cart page opens and 2 line item names are captured |
| 4 | Tap CONTINUE SHOPPING | The Products page is displayed again with the PRODUCTS header |
| 5 | Read the cart badge on the Products page | The cart badge still displays 2 |
| 6 | Reopen the cart and read the line item names | The cart still contains exactly the 2 names recorded in step 3 |

## Account

### SL-14 — Menu exposes all expected navigation items

**Priority:** P2
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testMenuItemsAreDisplayed`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Tap the hamburger menu control | The navigation menu opens |
| 3 | Check for the ALL ITEMS entry | The ALL ITEMS entry is displayed |
| 4 | Check for the WEBVIEW and QR CODE SCANNER entries | Both entries are displayed |
| 5 | Check for the GEO LOCATION and DRAWING entries | Both entries are displayed |
| 6 | Check for the ABOUT RESET APP STATE and LOGOUT entries | All three entries are displayed |
| 7 | Close the menu | The menu closes and the Products page is displayed again |

### SL-15 — Logout returns to the login screen with fields cleared

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testLogoutReturnsToLoginPage`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with a valid user | Products page opens and the PRODUCTS header is displayed |
| 2 | Tap the hamburger menu control | The navigation menu opens and the LOGOUT entry is displayed |
| 3 | Tap LOGOUT | The login page opens with the username and password fields displayed |
| 4 | Read the username field value | The username field is empty |
| 5 | Read the password field value | The password field is empty |
| 6 | Check that no error message is shown on the login page | No login error banner is displayed |

## Login

### SL-16 — Every valid user in the pool can log in and reach Products

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** standard_user / problem_user
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testLoginWithAllValidUsers`

| # | Action | Expected result |
|---|---|---|
| 1 | Log in with the valid user supplied by the data provider | Products page opens and the PRODUCTS header is displayed |
| 2 | Verify the product grid rendered for this user | Exactly 6 product cards are displayed |
| 3 | Check that no login error banner is present | No login error banner is displayed |
| 4 | Open the menu and tap LOGOUT | The login page opens with empty username and password fields |
| 5 | Repeat steps 1 to 4 for every remaining user in the valid pool | Every valid user in the pool (standard_user, problem_user) logs in successfully and logs out cleanly |

### SL-17 — Invalid username is rejected with field and banner errors

**Priority:** P1
&nbsp;&nbsp;·&nbsp;&nbsp;**Test data:** invalid_user / secret_sauce
&nbsp;&nbsp;·&nbsp;&nbsp;**Automated by:** `testLoginWithInvalidUsernameIsRejected`

| # | Action | Expected result |
|---|---|---|
| 1 | Open the app and confirm the login page is displayed | The login page opens with username password and LOGIN controls displayed |
| 2 | Enter an invalid username and a valid password then tap LOGIN | The login page remains displayed and no navigation occurs |
| 3 | Check whether the Products page opened | The Products page is NOT open |
| 4 | Check the error banner on the login page | The error banner is displayed with the text: Username and password do not match any user in this service. |
| 5 | Inspect the username field | A cross (X) error icon is displayed inside the username field |
| 6 | Inspect the password field | A cross (X) error icon is displayed inside the password field |
| 7 | Inspect the border colour of the username field | The username field border is rendered red |
| 8 | Inspect the border colour of the password field | The password field border is rendered red |
