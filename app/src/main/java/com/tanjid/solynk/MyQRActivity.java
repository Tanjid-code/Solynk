package com.tanjid.solynk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyQRActivity extends AppCompatActivity {

    private static final String TAG = "MyQRActivity";
    private static final int QR_CODE_SIZE = 800;

    private ImageView imageViewQR;
    private TextView textViewMyUserId;
    private MaterialButton buttonCopyId, buttonShare;
    private ProgressBar progressBar;

    private String myUserId;
    private String myPublicKey;
    private String fullConnectionString;

    private ExecutorService executorService;
    private Handler mainHandler;
    private Bitmap qrBitmap;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_my_qr);

            // Initialize executor and handler
            executorService = Executors.newSingleThreadExecutor();
            mainHandler = new Handler(Looper.getMainLooper());

            // Initialize UserManager
            userManager = new UserManager(this);

            // Setup toolbar
            setupToolbar();

            // Initialize views
            initializeViews();

            // Handle back button press (modern way)
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                }
            });

            // Load data and generate QR code
            loadUserData();

            if (fullConnectionString != null) {
                generateQRCode();
                setupButtons();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing QR activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("My Connection ID");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    private void initializeViews() {
        imageViewQR = findViewById(R.id.imageViewQR);
        textViewMyUserId = findViewById(R.id.textViewMyUserId);
        buttonCopyId = findViewById(R.id.buttonCopyId);
        buttonShare = findViewById(R.id.buttonShare);
        progressBar = findViewById(R.id.progressBarQR);

        if (imageViewQR == null || textViewMyUserId == null ||
                buttonCopyId == null || buttonShare == null || progressBar == null) {
            throw new IllegalStateException("Required views not found in layout");
        }

        Log.d(TAG, "All views initialized successfully");
    }

    private void loadUserData() {
        try {
            // Get user ID from UserManager
            myUserId = userManager.getUserId();

            if (myUserId == null || myUserId.isEmpty()) {
                Log.e(TAG, "UserManager returned null or empty userId");
                showErrorAndFinish("User ID not found. Please logout and login again.");
                return;
            }

            Log.d(TAG, "User ID loaded: " + myUserId);

            // Get public key
            myPublicKey = RSAEncryptionHelper.getPublicKeyString(this);

            if (myPublicKey == null || myPublicKey.isEmpty()) {
                Log.e(TAG, "Public key is null or empty");
                showErrorAndFinish("Encryption key not found. Please logout and login again.");
                return;
            }

            Log.d(TAG, "Public key loaded, length: " + myPublicKey.length());

            // Create connection string: userId|publicKey
            fullConnectionString = myUserId + "|" + myPublicKey;

            // Display user ID
            textViewMyUserId.setText(myUserId);

            Log.d(TAG, "User data loaded successfully. Connection string length: " + fullConnectionString.length());

        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
            showErrorAndFinish("Error loading user data: " + e.getMessage());
        }
    }

    private void generateQRCode() {
        if (fullConnectionString == null || fullConnectionString.isEmpty()) {
            Toast.makeText(this, "No connection data to generate QR code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        showProgress(true);

        // Generate QR code in background thread
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting QR code generation...");

                // Configure QR code hints
                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                hints.put(EncodeHintType.MARGIN, 1);

                // Generate QR code
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(
                        fullConnectionString,
                        BarcodeFormat.QR_CODE,
                        QR_CODE_SIZE,
                        QR_CODE_SIZE,
                        hints
                );

                // Create bitmap
                int width = bitMatrix.getWidth();
                int height = bitMatrix.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

                // Fill bitmap with QR code pixels
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                    }
                }

                // Store bitmap reference
                qrBitmap = bitmap;

                Log.d(TAG, "QR code bitmap created successfully");

                // Update UI on main thread
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        imageViewQR.setImageBitmap(bitmap);
                        showProgress(false);
                        Log.d(TAG, "QR code displayed successfully");
                        Toast.makeText(this, "QR code generated", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (WriterException e) {
                Log.e(TAG, "WriterException generating QR code", e);
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showProgress(false);
                        Toast.makeText(this, "Failed to generate QR code. Data might be too large.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error generating QR code", e);
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showProgress(false);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setupButtons() {
        // Copy ID button
        buttonCopyId.setOnClickListener(v -> {
            if (fullConnectionString == null || fullConnectionString.isEmpty()) {
                Toast.makeText(this, "No connection ID to copy", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Connection ID", fullConnectionString);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Connection ID copied to clipboard", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Connection ID copied to clipboard");
                } else {
                    Toast.makeText(this, "Clipboard not available", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error copying to clipboard", e);
                Toast.makeText(this, "Failed to copy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Share button
        buttonShare.setOnClickListener(v -> {
            if (fullConnectionString == null || fullConnectionString.isEmpty()) {
                Toast.makeText(this, "No connection ID to share", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Connect with me on Solynk");
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        "Connect with me on Solynk:\n\n" + fullConnectionString +
                                "\n\nCopy this entire text and paste it in the 'Connect Manually' field.");

                Intent chooser = Intent.createChooser(shareIntent, "Share Connection ID");
                startActivity(chooser);
                Log.d(TAG, "Share intent launched");

            } catch (Exception e) {
                Log.e(TAG, "Error sharing", e);
                Toast.makeText(this, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgress(boolean show) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (imageViewQR != null) {
                imageViewQR.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (buttonCopyId != null) {
                buttonCopyId.setEnabled(!show);
            }
            if (buttonShare != null) {
                buttonShare.setEnabled(!show);
            }
        });
    }

    private void showErrorAndFinish(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.e(TAG, message);
        });

        // Delay finish to allow toast to be visible
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2500);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // Recycle bitmap to free memory
        if (qrBitmap != null && !qrBitmap.isRecycled()) {
            qrBitmap.recycle();
            qrBitmap = null;
        }

        Log.d(TAG, "Activity destroyed and resources cleaned up");
    }
}