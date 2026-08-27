package com.mobile.swaglabs.qa.service;

import com.mobile.swaglabs.qa.pages.common.LoginPage;
import com.mobile.swaglabs.qa.pages.common.ProductsPage;

/**
 * The outcome of a login that was expected to be rejected.
 *
 * <p>Carries both halves an assertion needs: the {@link ProductsPage} that must not have opened,
 * and the {@link LoginPage} still on screen showing its error state.
 */
public class LoginAttempt {

    private final LoginPage loginPage;
    private final ProductsPage productsPage;

    LoginAttempt(LoginPage loginPage, ProductsPage productsPage) {
        this.loginPage = loginPage;
        this.productsPage = productsPage;
    }

    public LoginPage getLoginPage() {
        return loginPage;
    }

    public ProductsPage getProductsPage() {
        return productsPage;
    }
}
