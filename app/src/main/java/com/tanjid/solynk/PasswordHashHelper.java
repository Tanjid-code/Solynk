package com.tanjid.solynk;

import android.util.Base64;
import android.util.Log;

import org.signal.argon2.Argon2;
import org.signal.argon2.Argon2Exception;
import org.signal.argon2.MemoryCost;
import org.signal.argon2.Type;
import org.signal.argon2.Version;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Password hashing using Argon2id
 * Most secure password hashing algorithm - Winner of Password Hashing Competition
 * Android-compatible implementation using Signal's Argon2 library
 */
public class PasswordHashHelper {

    private static final String TAG = "PasswordHashHelper";

    // Argon2id configuration (OWASP recommended)
    private static final int ITERATIONS = 3;        // Number of iterations
    private static final int MEMORY_KB = 65536;     // 64 MB of memory
    private static final int PARALLELISM = 2;       // Number of parallel threads
    private static final int HASH_LENGTH = 32;      // 32 bytes = 256 bits
    private static final int SALT_LENGTH = 16;      // 16 bytes = 128 bits

    /**
     * Hash a password using Argon2id
     * Returns format: salt$hash (both Base64 encoded)
     *
     * @param password Plain text password
     * @return Encoded hash string
     */
    public static String hashPassword(String password) {
        try {
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty");
            }

            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash password with Argon2id
            Argon2 argon2 = new Argon2.Builder(Version.V13)
                    .type(Type.Argon2id)
                    .memoryCost(MemoryCost.KiB(MEMORY_KB))
                    .parallelism(PARALLELISM)
                    .iterations(ITERATIONS)
                    .hashLength(HASH_LENGTH)
                    .build();

            Argon2.Result result = argon2.hash(password.getBytes(StandardCharsets.UTF_8), salt);
            byte[] hash = result.getHash();

            // Encode as: salt$hash (both Base64)
            String saltEncoded = Base64.encodeToString(salt, Base64.NO_WRAP);
            String hashEncoded = Base64.encodeToString(hash, Base64.NO_WRAP);

            String combined = saltEncoded + "$" + hashEncoded;
            Log.d(TAG, "Password hashed successfully with Argon2id");
            return combined;

        } catch (Argon2Exception e) {
            Log.e(TAG, "Argon2 error during password hashing", e);
            throw new RuntimeException("Password hashing failed", e);
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verify a password against a stored hash
     *
     * @param password Plain text password to verify
     * @param storedHash The stored hash (format: salt$hash)
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            if (password == null || storedHash == null) {
                Log.w(TAG, "Password or hash is null");
                return false;
            }

            if (password.isEmpty() || storedHash.isEmpty()) {
                Log.w(TAG, "Password or hash is empty");
                return false;
            }

            // Parse stored hash (format: salt$hash)
            String[] parts = storedHash.split("\\$");
            if (parts.length != 2) {
                Log.e(TAG, "Invalid hash format. Expected: salt$hash");
                return false;
            }

            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[1], Base64.NO_WRAP);

            // Hash the input password with the same salt
            Argon2 argon2 = new Argon2.Builder(Version.V13)
                    .type(Type.Argon2id)
                    .memoryCost(MemoryCost.KiB(MEMORY_KB))
                    .parallelism(PARALLELISM)
                    .iterations(ITERATIONS)
                    .hashLength(HASH_LENGTH)
                    .build();

            Argon2.Result result = argon2.hash(password.getBytes(StandardCharsets.UTF_8), salt);
            byte[] actualHash = result.getHash();

            // Constant-time comparison
            boolean isValid = constantTimeArrayEquals(expectedHash, actualHash);
            Log.d(TAG, "Password verification: " + (isValid ? "SUCCESS" : "FAILED"));
            return isValid;

        } catch (Argon2Exception e) {
            Log.e(TAG, "Argon2 error during password verification", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error verifying password", e);
            return false;
        }
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks
     */
    private static boolean constantTimeArrayEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Get algorithm info for debugging
     */
    public static String getAlgorithmInfo() {
        return String.format(
                "Algorithm: Argon2id (v13), Iterations: %d, Memory: %d KB, Parallelism: %d, Hash: %d bytes, Salt: %d bytes",
                ITERATIONS, MEMORY_KB, PARALLELISM, HASH_LENGTH, SALT_LENGTH
        );
    }

    /**
     * @deprecated Not needed with Argon2id - salt is auto-generated in hashPassword()
     */
    @Deprecated
    public static String generateSalt() {
        Log.w(TAG, "generateSalt() is deprecated with Argon2id");
        return "";
    }

    /**
     * @deprecated Use hashPassword(String password) instead
     */
    @Deprecated
    public static String hashPassword(String password, String salt) {
        Log.w(TAG, "Using deprecated hashPassword method. Salt parameter ignored.");
        return hashPassword(password);
    }

    /**
     * @deprecated Use verifyPassword(String password, String storedHash) instead
     */
    @Deprecated
    public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
        Log.w(TAG, "Using deprecated verifyPassword method. Salt parameter ignored.");
        return verifyPassword(password, storedHash);
    }
}