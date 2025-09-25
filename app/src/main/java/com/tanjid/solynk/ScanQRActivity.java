package com.tanjid.solynk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ScanQRActivity extends AppCompatActivity {

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String scannedUserId = result.getContents();
                    SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);
                    // Save the ID of the user whose QR code was just scanned.
                    // This device now knows who it wants to chat with.
                    prefs.edit()
                            .putString("connectedUserId", scannedUserId)
                            .apply();

                    Toast.makeText(this, "Connected to user: " + scannedUserId.substring(0, 8) + "...", Toast.LENGTH_LONG).show();

                    // IMPROVED FLOW: Do NOT start ChatActivity here.
                    // Simply finish and return to MainActivity/previous screen.
                    // The user can then click a "Start Chat" button or similar.
                    // This separates the "connection" from "starting the chat".
                } else {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
                }
                // Finish this activity regardless of the outcome.
                finish();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This activity does not need a layout file as it only launches the scanner.
        launchScanner();
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a Solynk QR code to connect");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE); // Specify only QR codes
        barcodeLauncher.launch(options);
    }
}