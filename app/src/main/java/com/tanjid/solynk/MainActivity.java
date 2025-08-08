package com.tanjid.solynk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button buttonShowQR, buttonScanQR, buttonStartChat, buttonConnectManual, buttonDisconnect;
    private EditText editTextManualId;
    private TextView textViewConnectionStatus;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        buttonShowQR = findViewById(R.id.buttonShowQR);
        buttonScanQR = findViewById(R.id.buttonScanQR);
        buttonStartChat = findViewById(R.id.buttonStartChat);
        buttonConnectManual = findViewById(R.id.buttonConnectManual);
        buttonDisconnect = findViewById(R.id.buttonDisconnect);
        editTextManualId = findViewById(R.id.editTextManualId);
        textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus);

        prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);

        // Update connection status on start
        updateConnectionStatus();

        // Navigate to MyQRActivity
        buttonShowQR.setOnClickListener(v -> startActivity(new Intent(this, MyQRActivity.class)));

        // Navigate to ScanQRActivity
        buttonScanQR.setOnClickListener(v -> startActivity(new Intent(this, ScanQRActivity.class)));

        // Start Chat
        buttonStartChat.setOnClickListener(v -> {
            String connectedUserId = prefs.getString("connectedUserId", null);
            if (connectedUserId != null) {
                startActivity(new Intent(this, ChatActivity.class));
            } else {
                Toast.makeText(this, "Please connect to a user first.", Toast.LENGTH_SHORT).show();
            }
        });

        // Manual Connection
        buttonConnectManual.setOnClickListener(v -> {
            String manualId = editTextManualId.getText().toString().trim();
            if (!manualId.isEmpty()) {
                // Save the new connection ID
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("connectedUserId", manualId);
                editor.apply();
                Toast.makeText(this, "Manually connected to: " + manualId, Toast.LENGTH_SHORT).show();
                editTextManualId.setText("");
                updateConnectionStatus();
            } else {
                Toast.makeText(this, "Please enter a user ID.", Toast.LENGTH_SHORT).show();
            }
        });

        // Disconnect
        buttonDisconnect.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("connectedUserId");
            editor.apply();
            Toast.makeText(this, "Disconnected from current user.", Toast.LENGTH_SHORT).show();
            updateConnectionStatus();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        String connectedUserId = prefs.getString("connectedUserId", null);
        if (connectedUserId != null) {
            textViewConnectionStatus.setText("Status: Connected to " + connectedUserId);
            buttonStartChat.setEnabled(true);
            buttonDisconnect.setEnabled(true);
        } else {
            textViewConnectionStatus.setText("Status: Not Connected");
            buttonStartChat.setEnabled(false);
            buttonDisconnect.setEnabled(false);
        }
    }
}