package com.tanjid.solynk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ScanQRActivity extends AppCompatActivity {

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String scannedUserId = result.getContents();
                    SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("connectedUserId", scannedUserId)
                            .apply();

                    Toast.makeText(this, "Connected to user: " + scannedUserId, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(ScanQRActivity.this, ChatActivity.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(this, "Scan failed or cancelled", Toast.LENGTH_LONG).show();
                }
                finish();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureActivity.class);
        barcodeLauncher.launch(options);
    }
}