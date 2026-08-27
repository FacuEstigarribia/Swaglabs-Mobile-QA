package com.mobile.swaglabs.qa.enums;

import com.mobile.swaglabs.qa.data.UserData;
import com.mobile.swaglabs.qa.data.UserProvider;
import com.mobile.swaglabs.qa.service.UsersPool;

/**
 * The user pools declared in {@code _testdata.properties}.
 *
 * <p>Each constant is itself a {@link UserProvider}, so it can be handed straight to
 * {@code LoginService.login(...)}:
 *
 * <pre>{@code ProductsPage productsPage = getLoginService().login(VALID_USERS_POOL);}</pre>
 */
public enum UserPool implements UserProvider {

    /** Deterministic single user for the functional cases. */
    VALID_USERS_POOL("valid_users_pool"),

    /** Every account that can actually reach the Products page. */
    ALL_VALID_USERS_POOL("all_valid_users_pool"),

    /** Invalid credentials. */
    INVALID_USERS_POOL("invalid_users_pool");

    private final String value;

    UserPool(String value) {
        this.value = value;
    }

    /** Leases a user from this pool for the current thread. */
    @Override
    public UserData getUser() {
        return UsersPool.getInstance().getUser(this);
    }

    /** The {@code _testdata.properties} key backing this pool. */
    public String getValue() {
        return value;
    }
}
