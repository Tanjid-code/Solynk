package com.tanjid.solynk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

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

        // Check if user is registered
        if (!userManager.isRegistered()) {
            switchToRegisterMode();
        }

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
        editTextUsername.setText("");
        editTextPassword.setText("");
        editTextConfirmPassword.setText("");
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

        // Perform login in background (simulating async operation)
        new Thread(() -> {
            boolean success = userManager.login(username, password);

            runOnUiThread(() -> {
                showProgress(false);

                if (success) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    Toast.makeText(this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
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

        // Perform registration in background
        new Thread(() -> {
            boolean success = userManager.register(username, password);

            runOnUiThread(() -> {
                showProgress(false);

                if (success) {
                    Toast.makeText(this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show();
                    switchToLoginMode();
                } else {
                    Toast.makeText(this, getString(R.string.user_exists), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonSubmit.setEnabled(!show);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back if not registered
        if (!userManager.isRegistered()) {
            finishAffinity();
        } else {
            super.onBackPressed();
        }
    }
}