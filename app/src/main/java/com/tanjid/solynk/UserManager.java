package com.tanjid.solynk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.UUID;

public class UserManager {

    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "SoloConnectPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD_HASH = "passwordHash";
    private static final String KEY_USER_ID = "myUserId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences prefs;
    private Context context;

    public UserManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Register a new user
     */
    public boolean register(String username, String password) {
        // Check if user already exists
        if (prefs.contains(KEY_USERNAME)) {
            Log.e(TAG, "User already exists");
            return false;
        }

        // Hash password
        String passwordHash = PasswordHashHelper.hashPassword(password);

        // Generate unique user ID
        String userId = UUID.randomUUID().toString();

        // Generate RSA keys for this user
        RSAEncryptionHelper.generateAndStoreKeyPair(context);

        // Store user data
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD_HASH, passwordHash)
                .putString(KEY_USER_ID, userId)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();

        Log.d(TAG, "User registered successfully: " + username);
        return true;
    }

    /**
     * Login user
     */
    public boolean login(String username, String password) {
        String storedUsername = prefs.getString(KEY_USERNAME, null);
        String storedPasswordHash = prefs.getString(KEY_PASSWORD_HASH, null);

        if (storedUsername == null || storedPasswordHash == null) {
            Log.e(TAG, "No user registered");
            return false;
        }

        if (!storedUsername.equals(username)) {
            Log.e(TAG, "Username does not match");
            return false;
        }

        if (!PasswordHashHelper.verifyPassword(password, storedPasswordHash)) {
            Log.e(TAG, "Password verification failed");
            return false;
        }

        // Mark as logged in
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();
        Log.d(TAG, "User logged in successfully: " + username);
        return true;
    }

    /**
     * Logout user
     */
    public void logout() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove("connectedUserId")
                .remove("connectedUserPublicKey")
                .apply();
        Log.d(TAG, "User logged out");
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Check if user is registered
     */
    public boolean isRegistered() {
        return prefs.contains(KEY_USERNAME);
    }

    /**
     * Get current username
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
}