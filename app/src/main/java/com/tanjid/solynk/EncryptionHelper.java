package com.tanjid.solynk;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptionHelper {

    private static final String ALGORITHM = "AES";
    // IMPORTANT: This is your secret key. Both users MUST have the same key to decrypt messages.
    // For a production app, this key should be managed more securely (e.g., derived from user input,
    // or securely stored and retrieved, not hardcoded like this).
    // The key must be exactly 16, 24, or 32 bytes for AES. SHA-256 ensures a 32-byte key.
    private static final String SECRET_KEY_STRING = "SolynkSecretKey!@#ThisIsASecretKey"; // Make it longer to ensure unique SHA-256 output, though not strictly necessary for SHA-256 key generation itself.
    private static final String TAG = "EncryptionHelper";

    // Lazily initialized key to avoid recalculating it repeatedly
    private static SecretKeySpec secretKeySpec;

    private static SecretKeySpec getSecretKeySpec() throws NoSuchAlgorithmException {
        if (secretKeySpec == null) {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8);
            digest.update(bytes, 0, bytes.length);
            byte[] key = digest.digest(); // This generates a 32-byte (256-bit) key
            secretKeySpec = new SecretKeySpec(key, ALGORITHM);
        }
        return secretKeySpec;
    }

    public static String encrypt(String data) throws Exception {
        if (data == null || data.isEmpty()) {
            return ""; // Or throw IllegalArgumentException
        }
        try {
            SecretKeySpec secretKey = getSecretKeySpec();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedData, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed for data: '" + data + "' - " + e.getMessage(), e);
            throw new Exception("Encryption failed.", e); // Propagate the exception
        }
    }

    public static String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return ""; // Or throw IllegalArgumentException
        }
        try {
            SecretKeySpec secretKey = getSecretKeySpec();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed for data: '" + encryptedData + "' - " + e.getMessage(), e);
            throw new Exception("Decryption failed.", e); // Propagate the exception
        }
    }
}