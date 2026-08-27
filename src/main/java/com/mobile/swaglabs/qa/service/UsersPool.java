package com.mobile.swaglabs.qa.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobile.swaglabs.qa.data.UserData;
import com.mobile.swaglabs.qa.enums.UserPool;
import com.zebrunner.carina.utils.R;

/**
 * Thread-safe lease-and-release pool of test users, backed by {@code _testdata.properties}.
 */
public final class UsersPool {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String USERS_SPLITTER = "::";
    private static final String USER_LOGIN_PATTERN = "%s.login";
    private static final String USER_PASSWORD_PATTERN = "%s.password";

    private final Map<UserPool, ConcurrentLinkedQueue<UserData>> pools = new EnumMap<>(UserPool.class);
    private final Map<UserPool, List<UserData>> allUsers = new EnumMap<>(UserPool.class);
    private final ThreadLocal<Map<UserPool, UserData>> leased = ThreadLocal.withInitial(() -> new EnumMap<>(UserPool.class));
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition usersAvailable = lock.newCondition();

    private UsersPool() {
        LOGGER.info("Users pool initialization.");
        for (UserPool category : UserPool.values()) {
            initPool(category);
        }
    }

    private static final class Holder {
        private static final UsersPool INSTANCE = new UsersPool();
    }

    public static UsersPool getInstance() {
        return Holder.INSTANCE;
    }

    private void initPool(UserPool category) {
        String poolValue = R.TESTDATA.get(category.getValue());
        if (poolValue == null || poolValue.trim().isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Users pool has not been identified. Check that _testdata.properties contains '%s'.",
                    category.getValue()));
        }

        List<UserData> users = new ArrayList<>();
        for (String key : poolValue.split(USERS_SPLITTER)) {
            users.add(createUser(key.trim(), category));
        }
        LOGGER.info("Pool '{}' initialized with {} user(s).", category.getValue(), users.size());

        allUsers.put(category, Collections.unmodifiableList(users));
        pools.put(category, new ConcurrentLinkedQueue<>(users));
    }

    private UserData createUser(String key, UserPool category) {
        String login = R.TESTDATA.get(String.format(USER_LOGIN_PATTERN, key));
        String password = R.TESTDATA.get(String.format(USER_PASSWORD_PATTERN, key));
        if (login == null || login.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "User '%s' of pool '%s' has no '%s.login' entry in _testdata.properties.",
                    key, category.getValue(), key));
        }
        UserData user = new UserData(login, password);
        user.setCategory(category);
        return user;
    }

    /**
     * Leases a user from {@code category} for the current thread, blocking while the pool is
     * empty. Calling this twice for the same category on one thread returns the same user, so a
     * test that logs in more than once stays on a single account.
     */
    public UserData getUser(UserPool category) {
        Map<UserPool, UserData> held = leased.get();
        UserData current = held.get(category);
        if (current != null) {
            return current;
        }

        lock.lock();
        try {
            ConcurrentLinkedQueue<UserData> queue = pools.get(category);
            while (queue.isEmpty()) {
                LOGGER.info("Pool '{}' is empty; waiting for a user to be released.", category.getValue());
                usersAvailable.await();
            }
            UserData user = queue.poll();
            held.put(category, user);
            LOGGER.info("User '{}' leased from pool '{}'.", user.getLogin(), category.getValue());
            return user;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a user from " + category, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Every user configured for {@code category}, without leasing any of them.
     *
     * <p>For data providers such as SL-16's, which must enumerate a pool rather than consume it.
     */
    public List<UserData> getAllUsers(UserPool category) {
        return allUsers.get(category);
    }

    /** Returns every user this thread currently holds. Safe to call when it holds none. */
    public void releaseCurrentUsers() {
        Map<UserPool, UserData> held = leased.get();
        if (held.isEmpty()) {
            LOGGER.debug("No users to release for the current thread.");
            return;
        }

        lock.lock();
        try {
            held.forEach((category, user) -> {
                LOGGER.info("Releasing user '{}' back to pool '{}'.", user.getLogin(), category.getValue());
                pools.get(category).offer(user);
            });
            usersAvailable.signalAll();
        } finally {
            lock.unlock();
            leased.remove();
        }
    }
}
