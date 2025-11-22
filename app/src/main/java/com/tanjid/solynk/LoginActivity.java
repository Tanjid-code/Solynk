package com.tanjid.solynk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText editTextUsername, editTextPassword, editTextConfirmPassword;
    private TextInputLayout layoutConfirmPassword;
    private MaterialButton buttonSubmit;
    private TextView textViewTitle, textViewToggleLabel, textViewToggle;
    private ProgressBar progressBar;

    private UserManager userManager;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UserManager
        userManager = new UserManager(this);

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewToggleLabel = findViewById(R.id.textViewToggleLabel);
        textViewToggle = findViewById(R.id.textViewToggle);
        progressBar = findViewById(R.id.progressBar);

        // Default to login mode
        switchToLoginMode();

        // Submit button click
        buttonSubmit.setOnClickListener(v -> {
            if (isLoginMode) {
                performLogin();
            } else {
                performRegister();
            }
        });

        // Toggle between login and register
        textViewToggle.setOnClickListener(v -> {
            if (isLoginMode) {
                switchToRegisterMode();
            } else {
                switchToLoginMode();
            }
        });
    }

    private void switchToLoginMode() {
        isLoginMode = true;
        textViewTitle.setText(R.string.login);
        buttonSubmit.setText(R.string.login);
        layoutConfirmPassword.setVisibility(View.GONE);
        textViewToggleLabel.setText(R.string.no_account);
        textViewToggle.setText(R.string.register_here);
        clearFields();
    }

    private void switchToRegisterMode() {
        isLoginMode = false;
        textViewTitle.setText(R.string.register);
        buttonSubmit.setText(R.string.register);
        layoutConfirmPassword.setVisibility(View.VISIBLE);
        textViewToggleLabel.setText(R.string.have_account);
        textViewToggle.setText(R.string.login_here);
        clearFields();
    }

    private void clearFields() {
        if (editTextUsername != null) editTextUsername.setText("");
        if (editTextPassword != null) editTextPassword.setText("");
        if (editTextConfirmPassword != null) editTextConfirmPassword.setText("");
    }

    private void performLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validation
        if (username.isEmpty()) {
            editTextUsername.setError(getString(R.string.username_required));
            editTextUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError(getString(R.string.password_required));
            editTextPassword.requestFocus();
            return;
        }

        // Show progress
        showProgress(true);

        // Perform login via Firebase
        userManager.login(username, password, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showProgress(false);

                    // Validate encryption keys after successful login
                    validateKeysAndProceed();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Validate encryption keys after login
     */
    private void validateKeysAndProceed() {
        Log.d(TAG, "Validating encryption keys...");

        // Check if keys exist
        if (!RSAEncryptionHelper.keysExist(this)) {
            Log.w(TAG, "Keys don't exist, generating new keys...");
            RSAEncryptionHelper.generateAndStoreKeyPair(this);
        }

        // Validate key pair
        boolean keysValid = RSAEncryptionHelper.validateKeyPair(this);

        if (keysValid) {
            Log.d(TAG, "✓ Keys are valid");
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Log.e(TAG, "✗ Keys are INVALID!");

            // Show error dialog
            new AlertDialog.Builder(this)
                    .setTitle("Encryption Keys Invalid")
                    .setMessage("Your encryption keys are corrupted. Regenerating new keys...")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Regenerate keys
                        RSAEncryptionHelper.regenerateKeys(this);

                        // Update public key in Firebase
                        updatePublicKeyInFirebase();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * Update public key in Firebase after regeneration
     */
    private void updatePublicKeyInFirebase() {
        String username = userManager.getUsername();
        if (username == null) {
            Toast.makeText(this, "Error: Username not found", Toast.LENGTH_LONG).show();
            return;
        }

        String newPublicKey = RSAEncryptionHelper.getPublicKeyString(this);
        if (newPublicKey == null) {
            Toast.makeText(this, "Error: Failed to get new public key", Toast.LENGTH_LONG).show();
            return;
        }

        // Update in Firebase
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users")
                .child(username)
                .child("publicKey")
                .setValue(newPublicKey)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Public key updated in Firebase");
                    Toast.makeText(this, "Keys regenerated! Please share your QR code again.", Toast.LENGTH_LONG).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to update public key in Firebase", e);
                    Toast.makeText(this, "Error updating keys: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void performRegister() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validation
        if (username.isEmpty()) {
            editTextUsername.setError(getString(R.string.username_required));
            editTextUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            editTextUsername.setError("Username must be at least 3 characters");
            editTextUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError(getString(R.string.password_required));
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError(getString(R.string.password_length));
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError(getString(R.string.passwords_not_match));
            editTextConfirmPassword.requestFocus();
            return;
        }

        // Show progress
        showProgress(true);

        // Perform registration via Firebase
        userManager.register(username, password, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    switchToLoginMode();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (buttonSubmit != null) {
            buttonSubmit.setEnabled(!show);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}