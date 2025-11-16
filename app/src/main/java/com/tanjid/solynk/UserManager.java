package com.tanjid.solynk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {

    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "SoloConnectPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "myUserId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences prefs;
    private Context context;
    private DatabaseReference usersRef;

    public interface AuthCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public UserManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    /**
     * Register a new user in Firebase
     */
    public void register(String username, String password, AuthCallback callback) {
        // Input validation
        if (username == null || username.trim().isEmpty()) {
            callback.onFailure("Username cannot be empty");
            return;
        }
        if (password == null || password.length() < 6) {
            callback.onFailure("Password must be at least 6 characters");
            return;
        }

        final String cleanUsername = username.trim().toLowerCase();

        // Check if username already exists
        Query query = usersRef.orderByChild("username").equalTo(cleanUsername);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onFailure("Username already exists");
                    Log.w(TAG, "Registration failed: Username exists");
                } else {
                    // Username is available, proceed with registration
                    createUser(cleanUsername, password, callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure("Database error: " + error.getMessage());
                Log.e(TAG, "Database error during registration", error.toException());
            }
        });
    }

    /**
     * Create new user in Firebase
     */
    private void createUser(String username, String password, AuthCallback callback) {
        try {
            // Generate unique user ID
            String userId = UUID.randomUUID().toString();

            // Generate salt and hash password
            String salt = PasswordHashHelper.generateSalt();
            String passwordHash = PasswordHashHelper.hashPassword(password, salt);

            // Generate RSA keys
            RSAEncryptionHelper.generateAndStoreKeyPair(context);
            String publicKey = RSAEncryptionHelper.getPublicKeyString(context);

            if (publicKey == null) {
                callback.onFailure("Failed to generate encryption keys");
                return;
            }

            // Create user object
            User user = new User(
                    userId,
                    username,
                    passwordHash,
                    salt,
                    publicKey,
                    System.currentTimeMillis()
            );

            // Save to Firebase using username as key (for easy lookup)
            usersRef.child(username).setValue(user)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User registered successfully in Firebase: " + username);
                        callback.onSuccess("Registration successful!");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save user to Firebase", e);
                        callback.onFailure("Registration failed: " + e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error during user creation", e);
            callback.onFailure("Registration error: " + e.getMessage());
        }
    }

    /**
     * Login user by fetching from Firebase and verifying password
     */
    public void login(String username, String password, AuthCallback callback) {
        // Input validation
        if (username == null || username.trim().isEmpty()) {
            callback.onFailure("Username cannot be empty");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onFailure("Password cannot be empty");
            return;
        }

        final String cleanUsername = username.trim().toLowerCase();

        // Fetch user from Firebase
        usersRef.child(cleanUsername).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onFailure("User not found");
                    Log.w(TAG, "Login failed: User not found");
                    return;
                }

                try {
                    User user = snapshot.getValue(User.class);

                    if (user == null) {
                        callback.onFailure("Error loading user data");
                        return;
                    }

                    // Verify password
                    boolean passwordValid = PasswordHashHelper.verifyPassword(
                            password,
                            user.getPasswordHash(),
                            user.getSalt()
                    );

                    if (!passwordValid) {
                        callback.onFailure("Invalid password");
                        Log.w(TAG, "Login failed: Invalid password");
                        return;
                    }

                    // Password is correct - update last login
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastLogin", System.currentTimeMillis());
                    usersRef.child(cleanUsername).updateChildren(updates);

                    // Save session locally
                    prefs.edit()
                            .putString(KEY_USERNAME, user.getUsername())
                            .putString(KEY_USER_ID, user.getUserId())
                            .putBoolean(KEY_IS_LOGGED_IN, true)
                            .apply();

                    // Store public key locally
                    if (user.getPublicKey() != null) {
                        prefs.edit().putString("publicKey", user.getPublicKey()).apply();
                    }

                    Log.d(TAG, "User logged in successfully: " + cleanUsername);
                    callback.onSuccess("Login successful!");

                } catch (Exception e) {
                    Log.e(TAG, "Error processing login", e);
                    callback.onFailure("Login error: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure("Database error: " + error.getMessage());
                Log.e(TAG, "Database error during login", error.toException());
            }
        });
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
     * Check if user is registered (locally)
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

    /**
     * Delete account from Firebase
     */
    public void deleteAccount(AuthCallback callback) {
        String username = getUsername();
        if (username == null) {
            callback.onFailure("No user logged in");
            return;
        }

        usersRef.child(username).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Clear local data
                    prefs.edit().clear().apply();
                    Log.d(TAG, "Account deleted successfully");
                    callback.onSuccess("Account deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete account", e);
                    callback.onFailure("Failed to delete account: " + e.getMessage());
                });
    }
}