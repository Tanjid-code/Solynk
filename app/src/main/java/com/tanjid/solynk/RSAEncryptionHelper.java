package com.tanjid.solynk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class RSAEncryptionHelper {

    private static final String TAG = "RSAEncryptionHelper";
    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * Generate RSA key pair and store in SharedPreferences
     * Only generates if keys don't already exist
     */
    public static void generateAndStoreKeyPair(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("SoloConnectPrefs", Context.MODE_PRIVATE);

            // Check if keys already exist
            if (prefs.contains("publicKey") && prefs.contains("privateKey")) {
                Log.d(TAG, "Keys already exist, skipping generation");

                // Validate existing keys
                if (validateKeyPair(context)) {
                    Log.d(TAG, "✓ Existing keys are valid");
                    return;
                } else {
                    Log.w(TAG, "✗ Existing keys are invalid, regenerating...");
                    // Continue to generate new keys
                }
            }

            Log.d(TAG, "Generating new RSA key pair...");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Convert keys to Base64 strings
            String publicKeyString = Base64.encodeToString(
                    keyPair.getPublic().getEncoded(),
                    Base64.NO_WRAP
            );
            String privateKeyString = Base64.encodeToString(
                    keyPair.getPrivate().getEncoded(),
                    Base64.NO_WRAP
            );

            // Store in SharedPreferences
            prefs.edit()
                    .putString("publicKey", publicKeyString)
                    .putString("privateKey", privateKeyString)
                    .apply();

            Log.d(TAG, "✓ RSA key pair generated and stored successfully");

            // Validate the newly generated keys
            if (validateKeyPair(context)) {
                Log.d(TAG, "✓ New keys validated successfully");
            } else {
                Log.e(TAG, "✗ New keys validation failed!");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating key pair", e);
        }
    }

    /**
     * Validate that public and private keys match
     */
    public static boolean validateKeyPair(Context context) {
        try {
            String testMessage = "TEST_VALIDATION_" + System.currentTimeMillis();
            String publicKey = getPublicKeyString(context);

            if (publicKey == null) {
                Log.e(TAG, "Public key is null");
                return false;
            }

            // Encrypt with public key
            String encrypted = encrypt(testMessage, publicKey);

            // Decrypt with private key
            String decrypted = decrypt(context, encrypted);

            boolean isValid = testMessage.equals(decrypted);
            Log.d(TAG, "Key pair validation: " + (isValid ? "PASS" : "FAIL"));

            return isValid;

        } catch (Exception e) {
            Log.e(TAG, "Key validation error", e);
            return false;
        }
    }

    /**
     * Get public key as string from SharedPreferences
     */
    public static String getPublicKeyString(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SoloConnectPrefs", Context.MODE_PRIVATE);
        return prefs.getString("publicKey", null);
    }

    /**
     * Get private key from SharedPreferences
     */
    private static PrivateKey getPrivateKey(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("SoloConnectPrefs", Context.MODE_PRIVATE);
        String privateKeyString = prefs.getString("privateKey", null);

        if (privateKeyString == null) {
            throw new Exception("Private key not found");
        }

        byte[] keyBytes = Base64.decode(privateKeyString, Base64.NO_WRAP);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Convert public key string to PublicKey object
     */
    private static PublicKey getPublicKeyFromString(String publicKeyString) throws Exception {
        byte[] keyBytes = Base64.decode(publicKeyString, Base64.NO_WRAP);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * Encrypt message using recipient's public key
     */
    public static String encrypt(String message, String recipientPublicKeyString) throws Exception {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (recipientPublicKeyString == null) {
            throw new IllegalArgumentException("Recipient public key cannot be null");
        }

        PublicKey publicKey = getPublicKeyFromString(recipientPublicKeyString);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] messageBytes = message.getBytes("UTF-8");

        if (messageBytes.length > 190) {
            throw new Exception("Message too long for RSA encryption. Max 190 bytes.");
        }

        byte[] encryptedBytes = cipher.doFinal(messageBytes);
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
    }

    /**
     * Decrypt message using own private key
     */
    public static String decrypt(Context context, String encryptedMessage) throws Exception {
        if (encryptedMessage == null || encryptedMessage.isEmpty()) {
            throw new IllegalArgumentException("Encrypted message cannot be empty");
        }

        PrivateKey privateKey = getPrivateKey(context);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] encryptedBytes = Base64.decode(encryptedMessage, Base64.NO_WRAP);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, "UTF-8");
    }

    /**
     * Check if keys exist
     */
    public static boolean keysExist(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SoloConnectPrefs", Context.MODE_PRIVATE);
        return prefs.contains("publicKey") && prefs.contains("privateKey");
    }

    /**
     * Force regenerate keys (use when keys are corrupted)
     */
    public static void regenerateKeys(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SoloConnectPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .remove("publicKey")
                .remove("privateKey")
                .apply();

        generateAndStoreKeyPair(context);
    }
}