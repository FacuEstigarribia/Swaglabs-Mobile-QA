package com.mobile.swaglabs.qa.data;

import java.util.Objects;

import com.mobile.swaglabs.qa.enums.UserPool;

/** Credentials for one Swag Labs account, hydrated from {@code _testdata.properties}. */
public class UserData implements UserProvider {

    private String login;
    private String password;
    private UserPool category;

    public UserData() {
    }

    public UserData(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /** The pool this user was leased from, so it can be returned to the right queue. */
    public UserPool getCategory() {
        return category;
    }

    public void setCategory(UserPool category) {
        this.category = category;
    }

    @Override
    public UserData getUser() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserData)) {
            return false;
        }
        UserData that = (UserData) other;
        return Objects.equals(login, that.login) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login, password);
    }

    /** Password is deliberately omitted so it never reaches a log or a report. */
    @Override
    public String toString() {
        return "UserData [login=" + login + ", category=" + category + "]";
    }
}
