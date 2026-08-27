package com.mobile.swaglabs.qa.test;

import static com.mobile.swaglabs.qa.enums.UserPool.VALID_USERS_POOL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.mobile.swaglabs.qa.SwagLabsBaseTest;
import com.mobile.swaglabs.qa.data.Product;
import com.mobile.swaglabs.qa.enums.SortOption;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;
import com.zebrunner.carina.core.registrar.ownership.MethodOwner;
import com.zebrunner.carina.core.registrar.tag.Priority;
import com.zebrunner.carina.core.registrar.tag.TestPriority;
import com.zebrunner.carina.core.registrar.tag.TestTag;

/**
 * SL-05 .. SL-08 — the sort selector.
 *
 * <p>Each case asserts the displayed order against the same six products re-sorted locally by the
 * ordering the chosen option promises, so the expectation follows the catalog rather than
 * hard-coding product names that would break if the app's data changed.
 */
public class ProductFilterTest extends SwagLabsBaseTest {

    @Test(description = "Sort by Name A to Z")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestTag(name = "tcId", value = "SL-05")
    @TestTag(name = "feature", value = "Filtering")
    public void testSortByNameAscending() {
        verifySorting(SortOption.NAME_A_TO_Z);
    }

    @Test(description = "Sort by Name Z to A")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestTag(name = "tcId", value = "SL-06")
    @TestTag(name = "feature", value = "Filtering")
    public void testSortByNameDescending() {
        verifySorting(SortOption.NAME_Z_TO_A);
    }

    @Test(description = "Sort by Price low to high")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestTag(name = "tcId", value = "SL-07")
    @TestTag(name = "feature", value = "Filtering")
    public void testSortByPriceAscending() {
        verifySorting(SortOption.PRICE_LOW_TO_HIGH);
    }

    @Test(description = "Sort by Price high to low")
    @MethodOwner(owner = "festigarribia")
    @TestPriority(Priority.P1)
    @TestTag(name = "tcId", value = "SL-08")
    @TestTag(name = "feature", value = "Filtering")
    public void testSortByPriceDescending() {
        verifySorting(SortOption.PRICE_HIGH_TO_LOW);
    }

    /**
     * Logs in independently, applies {@code option}, and checks the resulting order.
     *
     * <p>Shared by the four cases because they differ only in the option applied; each still runs
     * its own login and its own hard page-opened assertion.
     */
    private void verifySorting(SortOption option) {
        ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        productsPage.sortBy(option);
        Assert.assertTrue(productsPage.isPageOpened(DEFAULT_TIMEOUT), PRODUCTS_PAGE_NOT_OPENED);

        List<Product> displayed = productsPage.getAllProducts();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(displayed.size(), EXPECTED_PRODUCT_COUNT, PRODUCT_COUNT_MISMATCH);

        List<Product> expected = new ArrayList<>(displayed);
        expected.sort(option.getExpectedOrder());

        softAssert.assertEquals(names(displayed), names(expected),
                String.format("Products are not ordered by '%s'! Displayed: %s",
                        option.getLabel(), names(displayed)));
        softAssert.assertAll();
    }

    private List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).collect(Collectors.toList());
    }
}
