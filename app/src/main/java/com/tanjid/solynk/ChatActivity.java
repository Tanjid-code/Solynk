package com.tanjid.solynk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    // UI Components
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private MaterialButton buttonSend;
    private LinearLayout emptyStateView;
    private ProgressBar progressBar;

    // Data
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private DatabaseReference messagesRef;
    private ChildEventListener messageListener;

    private String myUserId;
    private String myPublicKey;
    private String connectedUserId;
    private String connectedUserPublicKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "=== ChatActivity onCreate START ===");

            setContentView(R.layout.activity_chat);

            if (!loadUserData()) {
                return;
            }

            if (!initializeViews()) {
                return;
            }

            setupToolbar();
            setupRecyclerView();

            if (!initializeFirebase()) {
                return;
            }

            setupMessageListener();
            setupSendButton();
            updateEmptyState();

            Log.d(TAG, "=== ChatActivity initialized SUCCESSFULLY ===");

        } catch (Exception e) {
            Log.e(TAG, "!!! CRITICAL ERROR in onCreate !!!", e);
            showErrorAndFinish("Failed to start chat: " + e.getMessage());
        }
    }

    private void setupToolbar() {
        try {
            toolbar = findViewById(R.id.toolbar);

            if (toolbar == null) {
                Log.w(TAG, "WARNING: Toolbar not found");
                return;
            }

            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);

                if (connectedUserId != null) {
                    String displayId = connectedUserId.length() > 8
                            ? connectedUserId.substring(0, 8) + "..."
                            : connectedUserId;
                    getSupportActionBar().setTitle("Chat with " + displayId);
                } else {
                    getSupportActionBar().setTitle("Chat");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in setupToolbar: " + e.getMessage(), e);
        }
    }

    private boolean initializeViews() {
        try {
            recyclerView = findViewById(R.id.recyclerView);
            editTextMessage = findViewById(R.id.editTextMessage);
            buttonSend = findViewById(R.id.buttonSend);
            emptyStateView = findViewById(R.id.emptyStateView);
            progressBar = findViewById(R.id.progressBar);

            if (recyclerView == null || editTextMessage == null || buttonSend == null) {
                showErrorAndFinish("Critical views not found in layout");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Exception in initializeViews", e);
            showErrorAndFinish("View initialization error: " + e.getMessage());
            return false;
        }
    }

    private boolean loadUserData() {
        try {
            SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);

            myUserId = prefs.getString("myUserId", null);
            connectedUserId = prefs.getString("connectedUserId", null);
            connectedUserPublicKey = prefs.getString("connectedUserPublicKey", null);
            myPublicKey = RSAEncryptionHelper.getPublicKeyString(this);

            Log.d(TAG, "myUserId: " + (myUserId != null ? "EXISTS" : "NULL"));
            Log.d(TAG, "myPublicKey: " + (myPublicKey != null ? "EXISTS" : "NULL"));
            Log.d(TAG, "connectedUserId: " + (connectedUserId != null ? "EXISTS" : "NULL"));
            Log.d(TAG, "connectedUserPublicKey: " + (connectedUserPublicKey != null ? "EXISTS" : "NULL"));

            if (myUserId == null || myPublicKey == null) {
                showErrorAndFinish("Your user data not found. Please logout and login again.");
                return false;
            }

            if (connectedUserId == null || connectedUserPublicKey == null) {
                showErrorAndFinish("Connected user not found. Please connect via QR code first.");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Exception in loadUserData", e);
            showErrorAndFinish("Data loading error: " + e.getMessage());
            return false;
        }
    }

    private void setupRecyclerView() {
        try {
            // Pass context to adapter for decryption
            adapter = new MessageAdapter(messageList, myUserId, this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);
            Log.d(TAG, "RecyclerView configured with adapter");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            Toast.makeText(this, "Error setting up messages view", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean initializeFirebase() {
        try {
            String chatPath = "chats/" + getChatId(myUserId, connectedUserId);
            messagesRef = FirebaseDatabase.getInstance().getReference(chatPath);

            if (messagesRef == null) {
                showErrorAndFinish("Failed to initialize database connection");
                return false;
            }

            Log.d(TAG, "Firebase initialized - Path: " + chatPath);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error", e);
            showErrorAndFinish("Database error. Please check your internet connection.");
            return false;
        }
    }

    private void setupMessageListener() {
        showLoading(true);

        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                showLoading(false);

                try {
                    Message message = snapshot.getValue(Message.class);
                    if (message == null) {
                        Log.w(TAG, "Received null message from Firebase");
                        return;
                    }

                    // DO NOT decrypt here - let MessageAdapter handle decryption
                    // Just add the encrypted message to the list
                    messageList.add(message);

                    if (adapter != null) {
                        adapter.notifyItemInserted(messageList.size() - 1);
                    }
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    updateEmptyState();

                    Log.d(TAG, "Message added to list (encrypted)");

                } catch (Exception e) {
                    Log.e(TAG, "Error processing incoming message", e);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "Message changed");
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Message removed");
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "Message moved");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e(TAG, "Firebase listener error: " + error.getMessage());
                Toast.makeText(ChatActivity.this,
                        "Connection error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        if (messagesRef != null) {
            messagesRef.addChildEventListener(messageListener);
            Log.d(TAG, "Message listener attached to Firebase");
        }
    }

    private void setupSendButton() {
        if (buttonSend != null) {
            buttonSend.setOnClickListener(v -> sendMessage());
            Log.d(TAG, "Send button listener attached");
        }
    }

    private void sendMessage() {
        if (editTextMessage == null) {
            Toast.makeText(this, "Error: Input field not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String plainText = editTextMessage.getText().toString().trim();

        if (plainText.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (plainText.length() > 190) {
            Toast.makeText(this, "Message too long. Maximum 190 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (buttonSend != null) {
            buttonSend.setEnabled(false);
        }

        try {
            Log.d(TAG, "=== ENCRYPTION DEBUG ===");
            Log.d(TAG, "Plain text: " + plainText);
            Log.d(TAG, "My UserId: " + myUserId);
            Log.d(TAG, "Connected UserId: " + connectedUserId);

            // CHECK 1: Verify keys exist
            if (myPublicKey == null || myPublicKey.isEmpty()) {
                throw new Exception("Your public key is missing!");
            }
            if (connectedUserPublicKey == null || connectedUserPublicKey.isEmpty()) {
                throw new Exception("Connected user's public key is missing!");
            }

            Log.d(TAG, "My Public Key (first 50): " + myPublicKey.substring(0, Math.min(50, myPublicKey.length())));
            Log.d(TAG, "Connected Public Key (first 50): " + connectedUserPublicKey.substring(0, Math.min(50, connectedUserPublicKey.length())));

            // CHECK 2: Are the keys the same? (They should be DIFFERENT!)
            if (myPublicKey.equals(connectedUserPublicKey)) {
                Toast.makeText(this, "ERROR: You're using the same key for both users!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "✗✗✗ SAME PUBLIC KEY ERROR ✗✗✗");
                if (buttonSend != null) buttonSend.setEnabled(true);
                return;
            }

            Log.d(TAG, "✓ Keys are different (correct!)");

            // Encrypt with RECEIVER's public key
            String encryptedForReceiver = RSAEncryptionHelper.encrypt(plainText, connectedUserPublicKey);
            Log.d(TAG, "✓ Encrypted for receiver, length: " + encryptedForReceiver.length());

            // Encrypt with MY public key (sender copy)
            String encryptedForSender = RSAEncryptionHelper.encrypt(plainText, myPublicKey);
            Log.d(TAG, "✓ Encrypted for sender, length: " + encryptedForSender.length());

            // CHECK 3: Test decrypt immediately (THIS IS THE KEY TEST!)
            try {
                String testDecrypt = RSAEncryptionHelper.decrypt(this, encryptedForSender);
                if (testDecrypt.equals(plainText)) {
                    Log.d(TAG, "✓✓✓ TEST DECRYPT WORKS! Message: " + testDecrypt);
                    Toast.makeText(this, "Encryption test passed!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "✗✗✗ DECRYPTED TEXT DOESN'T MATCH!");
                    Toast.makeText(this, "ERROR: Decryption mismatch!", Toast.LENGTH_LONG).show();
                    if (buttonSend != null) buttonSend.setEnabled(true);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "✗✗✗ TEST DECRYPT FAILED", e);
                Toast.makeText(this, "ERROR: Cannot decrypt with your own key! " + e.getMessage(), Toast.LENGTH_LONG).show();
                if (buttonSend != null) buttonSend.setEnabled(true);
                return;
            }

            // Create message with BOTH encrypted versions
            Message message = new Message(
                    encryptedForReceiver,    // text - for receiver to decrypt
                    encryptedForSender,      // senderCopy - for sender to decrypt
                    myUserId,
                    connectedUserId,
                    System.currentTimeMillis(),
                    myPublicKey
            );

            Log.d(TAG, "Sending to Firebase...");
            if (messagesRef != null) {
                messagesRef.push().setValue(message).addOnCompleteListener(task -> {
                    if (buttonSend != null) {
                        buttonSend.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        if (editTextMessage != null) {
                            editTextMessage.setText("");
                        }
                        Toast.makeText(this, "✓ Message sent", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "✓ Message sent successfully");
                    } else {
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "✗ Message send failed", task.getException());
                    }
                });
            }

        } catch (Exception e) {
            if (buttonSend != null) {
                buttonSend.setEnabled(true);
            }
            Log.e(TAG, "Message encryption/send error", e);
            Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getChatId(String user1, String user2) {
        if (user1 == null || user2 == null) {
            return "unknown_chat";
        }
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    private void updateEmptyState() {
        try {
            if (emptyStateView != null && recyclerView != null) {
                if (messageList.isEmpty()) {
                    emptyStateView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating empty state", e);
        }
    }

    private void showLoading(boolean show) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling progress bar", e);
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "ERROR - Finishing activity: " + message);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (messageListener != null && messagesRef != null) {
                messagesRef.removeEventListener(messageListener);
                Log.d(TAG, "Message listener removed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing listener in onDestroy", e);
        }
    }
}