package com.tanjid.solynk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI Components
    private MaterialButton buttonShowQR, buttonScanQR, buttonStartChat,
            buttonConnectManual, buttonDisconnect, buttonLogout;
    private TextInputEditText editTextManualId;
    private TextView textViewConnectionStatus, textViewUsername, textViewMyUserId;
    private ProgressBar progressBar;

    // Data
    private SharedPreferences prefs;
    private UserManager userManager;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize executor and handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize UserManager
        userManager = new UserManager(this);

        // Check if user is logged in
        if (!userManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Display user information
        displayUserInfo();

        // Setup copy functionality for User ID
        setupUserIdCopyFunction();

        // Ensure RSA keys exist (in background)
        ensureRSAKeysExist();

        // Setup button listeners
        setupButtonListeners();

        // Update connection status
        updateConnectionStatus();
    }

    private void initializeViews() {
        try {
            buttonShowQR = findViewById(R.id.buttonShowQR);
            buttonScanQR = findViewById(R.id.buttonScanQR);
            buttonStartChat = findViewById(R.id.buttonStartChat);
            buttonConnectManual = findViewById(R.id.buttonConnectManual);
            buttonDisconnect = findViewById(R.id.buttonDisconnect);
            buttonLogout = findViewById(R.id.buttonLogout);
            editTextManualId = findViewById(R.id.editTextManualId);
            textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus);
            textViewUsername = findViewById(R.id.textViewUsername);
            textViewMyUserId = findViewById(R.id.textViewMyUserId);
            progressBar = findViewById(R.id.progressBarMain);

            // Validate critical views
            if (buttonShowQR == null || buttonScanQR == null || buttonStartChat == null) {
                throw new IllegalStateException("Critical views not found in layout");
            }

            Log.d(TAG, "All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void displayUserInfo() {
        String username = userManager.getUsername();
        String userId = userManager.getUserId();

        if (textViewUsername != null) {
            textViewUsername.setText("Logged in as: " + (username != null ? username : "Unknown"));
        }

        if (textViewMyUserId != null && userId != null) {
            String shortId = userId.length() > 16 ? userId.substring(0, 16) + "..." : userId;
            textViewMyUserId.setText("My ID: " + shortId + " (Tap to copy)");
        }

        Log.d(TAG, "User info displayed - Username: " + username);
    }

    private void setupUserIdCopyFunction() {
        if (textViewMyUserId != null) {
            textViewMyUserId.setOnClickListener(v -> {
                String userId = userManager.getUserId();

                if (userId != null && !userId.isEmpty()) {
                    // Copy to clipboard
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("User ID", userId);
                    clipboard.setPrimaryClip(clip);

                    // Show confirmation
                    Toast.makeText(this, "User ID copied to clipboard!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User ID copied to clipboard");

                    // Optional: Visual feedback (brief highlight)
                    textViewMyUserId.setAlpha(0.5f);
                    textViewMyUserId.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                } else {
                    Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Attempted to copy null or empty User ID");
                }
            });

            Log.d(TAG, "User ID copy functionality set up");
        }
    }

    private void ensureRSAKeysExist() {
        showProgress(true);

        executorService.execute(() -> {
            try {
                // Check if keys exist in SharedPreferences
                String publicKey = prefs.getString("publicKey", null);

                if (publicKey == null || publicKey.isEmpty()) {
                    Log.d(TAG, "RSA keys not found in local storage, generating...");

                    // Generate new keys
                    RSAEncryptionHelper.generateAndStoreKeyPair(this);
                    publicKey = RSAEncryptionHelper.getPublicKeyString(this);

                    // Update user's public key in Firebase
                    updatePublicKeyInFirebase(publicKey);

                    mainHandler.post(() -> {
                        Toast.makeText(this, "Encryption keys generated", Toast.LENGTH_SHORT).show();
                        showProgress(false);
                    });
                } else {
                    Log.d(TAG, "RSA keys already exist");
                    mainHandler.post(() -> showProgress(false));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking/generating RSA keys", e);
                mainHandler.post(() -> {
                    showProgress(false);
                    Toast.makeText(this, "Error with encryption keys: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updatePublicKeyInFirebase(String publicKey) {
        String username = userManager.getUsername();
        if (username != null && publicKey != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(username);

            userRef.child("publicKey").setValue(publicKey)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Public key updated in Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update public key", e));
        }
    }

    private void setupButtonListeners() {
        // Show QR Code
        buttonShowQR.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Show QR button clicked");
                Intent intent = new Intent(this, MyQRActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening MyQRActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Scan QR Code
        buttonScanQR.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Scan QR button clicked");
                Intent intent = new Intent(this, ScanQRActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening ScanQRActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Start Chat
        buttonStartChat.setOnClickListener(v -> {
            String connectedUserId = prefs.getString("connectedUserId", null);
            String connectedUserPublicKey = prefs.getString("connectedUserPublicKey", null);

            if (connectedUserId != null && connectedUserPublicKey != null) {
                try {
                    Log.d(TAG, "Starting chat with user: " + connectedUserId);
                    Intent intent = new Intent(this, ChatActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening ChatActivity", e);
                    Toast.makeText(this, "Error starting chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Please connect to a user first", Toast.LENGTH_SHORT).show();
            }
        });

        // Connect Manually
        buttonConnectManual.setOnClickListener(v -> connectManually());

        // Disconnect
        buttonDisconnect.setOnClickListener(v -> showDisconnectConfirmation());

        // Logout
        buttonLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void connectManually() {
        if (editTextManualId == null) {
            Toast.makeText(this, "Input field not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String manualId = editTextManualId.getText().toString().trim();

        if (manualId.isEmpty()) {
            editTextManualId.setError("Please enter a connection ID");
            editTextManualId.requestFocus();
            return;
        }

        // Validate format: userId|publicKey
        if (!manualId.contains("|")) {
            editTextManualId.setError("Invalid format. Should be: userId|publicKey");
            Toast.makeText(this, "Invalid connection ID format. Please copy the complete ID.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] parts = manualId.split("\\|", 2);

        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            editTextManualId.setError("Invalid connection ID");
            Toast.makeText(this, "Connection ID is incomplete or corrupted", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if trying to connect to self
        String myUserId = userManager.getUserId();
        if (parts[0].equals(myUserId)) {
            Toast.makeText(this, "You cannot connect to yourself!", Toast.LENGTH_SHORT).show();
            editTextManualId.setText("");
            return;
        }

        // Save connection
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("connectedUserId", parts[0]);
        editor.putString("connectedUserPublicKey", parts[1]);
        editor.apply();

        String shortId = parts[0].length() > 8 ? parts[0].substring(0, 8) + "..." : parts[0];
        Toast.makeText(this, "Connected to: " + shortId, Toast.LENGTH_SHORT).show();
        editTextManualId.setText("");

        Log.d(TAG, "Manually connected to user: " + parts[0]);
        updateConnectionStatus();
    }

    private void showDisconnectConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Disconnect")
                .setMessage("Are you sure you want to disconnect from this user?")
                .setPositiveButton("Yes", (dialog, which) -> performDisconnect())
                .setNegativeButton("No", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performDisconnect() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("connectedUserId");
        editor.remove("connectedUserPublicKey");
        editor.apply();

        Toast.makeText(this, "Disconnected successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "User disconnected");
        updateConnectionStatus();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout? You will need to login again to use the app.")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void performLogout() {
        userManager.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "User logged out");
        navigateToLogin();
    }

    private void updateConnectionStatus() {
        String connectedUserId = prefs.getString("connectedUserId", null);
        String connectedUserPublicKey = prefs.getString("connectedUserPublicKey", null);

        if (textViewConnectionStatus == null) {
            Log.w(TAG, "Connection status TextView is null");
            return;
        }

        if (connectedUserId != null && connectedUserPublicKey != null) {
            // Connected
            String shortId = connectedUserId.length() > 8
                    ? connectedUserId.substring(0, 8) + "..."
                    : connectedUserId;

            textViewConnectionStatus.setText("Connected to: " + shortId);
            textViewConnectionStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.status_connected)
            );

            if (buttonStartChat != null) buttonStartChat.setEnabled(true);
            if (buttonDisconnect != null) buttonDisconnect.setEnabled(true);

            Log.d(TAG, "Status: Connected to " + connectedUserId);
        } else {
            // Not connected
            textViewConnectionStatus.setText(R.string.status_not_connected);
            textViewConnectionStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.status_disconnected)
            );

            if (buttonStartChat != null) buttonStartChat.setEnabled(false);
            if (buttonDisconnect != null) buttonDisconnect.setEnabled(false);

            Log.d(TAG, "Status: Not connected");
        }
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
        Log.d(TAG, "Activity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        Log.d(TAG, "Activity destroyed");
    }

    @Override
    public void onBackPressed() {
        // Show exit confirmation
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Do you want to exit Solynk?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    super.onBackPressed();
                    finishAffinity(); // Close all activities
                })
                .setNegativeButton("No", null)
                .show();
    }
}