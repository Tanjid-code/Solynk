package com.tanjid.solynk;

import android.util.Base64;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing using PBKDF2WithHmacSHA256
 * Much more secure than BCrypt and built into Android
 */
public class PasswordHashHelper {

    private static final String TAG = "PasswordHashHelper";

    // PBKDF2 configuration
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100000; // NIST recommends 100,000+ iterations
    private static final int KEY_LENGTH = 256; // 256 bits = 32 bytes
    private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits

    /**
     * Generate a random salt
     */
    public static String generateSalt() {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            return Base64.encodeToString(salt, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error generating salt", e);
            throw new RuntimeException("Failed to generate salt", e);
        }
    }

    /**
     * Hash a password with the given salt using PBKDF2
     *
     * @param password Plain text password
     * @param salt Base64 encoded salt
     * @return Base64 encoded hash
     */
    public static String hashPassword(String password, String salt) {
        try {
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty");
            }
            if (salt == null || salt.isEmpty()) {
                throw new IllegalArgumentException("Salt cannot be empty");
            }

            byte[] saltBytes = Base64.decode(salt, Base64.NO_WRAP);

            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    saltBytes,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.encodeToString(hash, Base64.NO_WRAP);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error hashing password", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verify a password against a stored hash
     *
     * @param password Plain text password to verify
     * @param storedHash Base64 encoded stored hash
     * @param storedSalt Base64 encoded stored salt
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
        try {
            if (password == null || storedHash == null || storedSalt == null) {
                return false;
            }

            // Hash the input password with the stored salt
            String computedHash = hashPassword(password, storedSalt);

            // Compare hashes (constant-time comparison)
            return constantTimeEquals(computedHash, storedHash);

        } catch (Exception e) {
            Log.e(TAG, "Error verifying password", e);
            return false;
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * Get algorithm info for debugging
     */
    public static String getAlgorithmInfo() {
        return String.format(
                "Algorithm: %s, Iterations: %d, Key Length: %d bits, Salt Length: %d bytes",
                ALGORITHM, ITERATIONS, KEY_LENGTH, SALT_LENGTH
        );
    }
}