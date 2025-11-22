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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserManager {

    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "SoloConnectPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "myUserId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences prefs;
    private Context context;
    private DatabaseReference usersRef;
    private ExecutorService executorService;

    public interface AuthCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public UserManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        this.executorService = Executors.newSingleThreadExecutor();
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
    /**
     * Create new user in Firebase
     */
    private void createUser(String username, String password, AuthCallback callback) {
        // Run password hashing in background thread (Argon2 is CPU-intensive)
        executorService.execute(() -> {
            try {
                // Generate unique user ID
                String userId = UUID.randomUUID().toString();

                // Hash password with Argon2id (salt is included in the hash)
                Log.d(TAG, "Hashing password with Argon2id...");
                String passwordHash = PasswordHashHelper.hashPassword(password);
                Log.d(TAG, "Password hashed successfully");

                // Generate RSA keys (only if they don't exist or are invalid)
                Log.d(TAG, "Checking RSA keys...");
                RSAEncryptionHelper.generateAndStoreKeyPair(context);

                // Validate keys before proceeding
                if (!RSAEncryptionHelper.validateKeyPair(context)) {
                    callback.onFailure("Failed to generate valid encryption keys. Please try again.");
                    return;
                }

                String publicKey = RSAEncryptionHelper.getPublicKeyString(context);

                if (publicKey == null) {
                    callback.onFailure("Failed to retrieve encryption keys");
                    return;
                }

                Log.d(TAG, "✓ RSA keys validated successfully");

                // Create user object (NO SEPARATE SALT - it's in the hash)
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", userId);
                userData.put("username", username);
                userData.put("passwordHash", passwordHash);
                userData.put("publicKey", publicKey);
                userData.put("createdAt", System.currentTimeMillis());

                // Save to Firebase using username as key
                usersRef.child(username).setValue(userData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "User registered successfully: " + username);

                            // Save userId locally
                            prefs.edit().putString(KEY_USER_ID, userId).apply();

                            callback.onSuccess("Registration successful! Please login.");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to save user to Firebase", e);
                            callback.onFailure("Registration failed: " + e.getMessage());
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error during user creation", e);
                callback.onFailure("Registration error: " + e.getMessage());
            }
        });
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

                // Run password verification in background thread
                executorService.execute(() -> {
                    try {
                        String userId = snapshot.child("userId").getValue(String.class);
                        String storedUsername = snapshot.child("username").getValue(String.class);
                        String passwordHash = snapshot.child("passwordHash").getValue(String.class);
                        String publicKey = snapshot.child("publicKey").getValue(String.class);

                        if (passwordHash == null) {
                            callback.onFailure("Error loading user data");
                            return;
                        }

                        // Verify password with Argon2id
                        Log.d(TAG, "Verifying password...");
                        boolean passwordValid = PasswordHashHelper.verifyPassword(password, passwordHash);

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
                                .putString(KEY_USERNAME, storedUsername != null ? storedUsername : cleanUsername)
                                .putString(KEY_USER_ID, userId)
                                .putBoolean(KEY_IS_LOGGED_IN, true)
                                .apply();

                        // Store public key locally
                        if (publicKey != null) {
                            prefs.edit().putString("publicKey", publicKey).apply();
                        }

                        Log.d(TAG, "User logged in successfully: " + cleanUsername);
                        callback.onSuccess("Login successful!");

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing login", e);
                        callback.onFailure("Login error: " + e.getMessage());
                    }
                });
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

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}