package com.tanjid.solynk;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ScanQRActivity extends AppCompatActivity {

    private static final String TAG = "ScanQRActivity";

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String scannedData = result.getContents();
                    Log.d(TAG, "Scanned data: " + scannedData);

                    // Expected format: userId|publicKey
                    if (scannedData.contains("|")) {
                        String[] parts = scannedData.split("\\|", 2);

                        if (parts.length == 2) {
                            String scannedUserId = parts[0];
                            String scannedPublicKey = parts[1];

                            SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("connectedUserId", scannedUserId)
                                    .putString("connectedUserPublicKey", scannedPublicKey)
                                    .apply();

                            String shortId = scannedUserId.length() > 8 ?
                                    scannedUserId.substring(0, 8) + "..." : scannedUserId;
                            Toast.makeText(this, "Connected to user: " + shortId, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                }
                finish();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check camera permission
        if (PermissionHelper.hasCameraPermission(this)) {
            launchScanner();
        } else {
            PermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, launch scanner
                launchScanner();
            } else {
                // Permission denied
                showPermissionDeniedDialog();
            }
        }
    }

    private void launchScanner() {
        try {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a Solynk QR code to connect");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setCameraId(0); // Use back camera
            options.setBarcodeImageEnabled(false);

            barcodeLauncher.launch(options);
        } catch (Exception e) {
            Log.e(TAG, "Error launching scanner", e);
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("Camera permission is required to scan QR codes. Please grant permission in app settings.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}