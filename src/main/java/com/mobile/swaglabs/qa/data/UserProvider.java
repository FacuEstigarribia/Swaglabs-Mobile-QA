package com.mobile.swaglabs.qa.data;

/**
 * Anything that can yield a {@link UserData}.
 *
 * <p>Implemented both by {@link UserData} itself (returning {@code this}) and by
 * {@code UserPool} constants (leasing a user from the pool). That lets a service accept either a
 * concrete user or a pool without overload ambiguity.
 */
public interface UserProvider {

    UserData getUser();
}
