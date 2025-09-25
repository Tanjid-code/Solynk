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

            // Step 1: Set content view
            Log.d(TAG, "Step 1: Setting content view");
            setContentView(R.layout.activity_chat);

            // Step 2: Load user data FIRST (before initializing views)
            Log.d(TAG, "Step 2: Loading user data");
            if (!loadUserData()) {
                Log.e(TAG, "FAILED: User data loading failed");
                return;
            }
            Log.d(TAG, "SUCCESS: User data loaded");

            // Step 3: Initialize views
            Log.d(TAG, "Step 3: Initializing views");
            if (!initializeViews()) {
                Log.e(TAG, "FAILED: Views initialization failed");
                return;
            }
            Log.d(TAG, "SUCCESS: All views initialized");

            // Step 4: Setup toolbar (non-critical)
            Log.d(TAG, "Step 4: Setting up toolbar");
            setupToolbar();
            Log.d(TAG, "SUCCESS: Toolbar setup complete");

            // Step 5: Setup RecyclerView
            Log.d(TAG, "Step 5: Setting up RecyclerView");
            setupRecyclerView();
            Log.d(TAG, "SUCCESS: RecyclerView setup complete");

            // Step 6: Initialize Firebase
            Log.d(TAG, "Step 6: Initializing Firebase");
            if (!initializeFirebase()) {
                Log.e(TAG, "FAILED: Firebase initialization failed");
                return;
            }
            Log.d(TAG, "SUCCESS: Firebase initialized");

            // Step 7: Setup message listener
            Log.d(TAG, "Step 7: Setting up message listener");
            setupMessageListener();
            Log.d(TAG, "SUCCESS: Message listener setup complete");

            // Step 8: Setup send button
            Log.d(TAG, "Step 8: Setting up send button");
            setupSendButton();
            Log.d(TAG, "SUCCESS: Send button setup complete");

            // Step 9: Update empty state
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
                Log.w(TAG, "WARNING: Toolbar not found - continuing without it");
                // Try to set title using default ActionBar
                try {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        getSupportActionBar().setTitle("Chat");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not set default ActionBar", e);
                }
                return;
            }

            Log.d(TAG, "Toolbar found, setting as ActionBar");
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);

                // Set title with connected user info
                if (connectedUserId != null) {
                    String displayId = connectedUserId.length() > 8
                            ? connectedUserId.substring(0, 8) + "..."
                            : connectedUserId;
                    getSupportActionBar().setTitle("Chat with " + displayId);
                } else {
                    getSupportActionBar().setTitle("Chat");
                }

                Log.d(TAG, "ActionBar configured successfully");
            } else {
                Log.w(TAG, "getSupportActionBar() returned null");
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in setupToolbar (non-critical): " + e.getMessage(), e);
            // Don't crash - toolbar is not critical for functionality
        }
    }

    private boolean initializeViews() {
        try {
            Log.d(TAG, "Finding views by ID...");

            recyclerView = findViewById(R.id.recyclerView);
            Log.d(TAG, "recyclerView: " + (recyclerView != null ? "✓ FOUND" : "✗ NULL"));

            editTextMessage = findViewById(R.id.editTextMessage);
            Log.d(TAG, "editTextMessage: " + (editTextMessage != null ? "✓ FOUND" : "✗ NULL"));

            buttonSend = findViewById(R.id.buttonSend);
            Log.d(TAG, "buttonSend: " + (buttonSend != null ? "✓ FOUND" : "✗ NULL"));

            emptyStateView = findViewById(R.id.emptyStateView);
            Log.d(TAG, "emptyStateView: " + (emptyStateView != null ? "✓ FOUND" : "✗ NULL"));

            progressBar = findViewById(R.id.progressBar);
            Log.d(TAG, "progressBar: " + (progressBar != null ? "✓ FOUND" : "✗ NULL"));

            // Check critical views
            if (recyclerView == null) {
                showErrorAndFinish("RecyclerView not found in layout. Please check activity_chat.xml");
                return false;
            }

            if (editTextMessage == null) {
                showErrorAndFinish("Message input not found in layout. Please check activity_chat.xml");
                return false;
            }

            if (buttonSend == null) {
                showErrorAndFinish("Send button not found in layout. Please check activity_chat.xml");
                return false;
            }

            Log.d(TAG, "All critical views found successfully");
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

            try {
                myPublicKey = RSAEncryptionHelper.getPublicKeyString(this);
            } catch (Exception e) {
                Log.e(TAG, "Error getting public key", e);
                myPublicKey = null;
            }

            Log.d(TAG, "myUserId: " + (myUserId != null ? "✓ EXISTS (" + myUserId.substring(0, Math.min(8, myUserId.length())) + "...)" : "✗ NULL"));
            Log.d(TAG, "myPublicKey: " + (myPublicKey != null ? "✓ EXISTS" : "✗ NULL"));
            Log.d(TAG, "connectedUserId: " + (connectedUserId != null ? "✓ EXISTS (" + connectedUserId.substring(0, Math.min(8, connectedUserId.length())) + "...)" : "✗ NULL"));
            Log.d(TAG, "connectedUserPublicKey: " + (connectedUserPublicKey != null ? "✓ EXISTS" : "✗ NULL"));

            if (myUserId == null || myUserId.isEmpty()) {
                showErrorAndFinish("Your user ID not found. Please logout and login again.");
                return false;
            }

            if (myPublicKey == null || myPublicKey.isEmpty()) {
                showErrorAndFinish("Your encryption key not found. Please logout and login again.");
                return false;
            }

            if (connectedUserId == null || connectedUserId.isEmpty()) {
                showErrorAndFinish("Connected user not found. Please connect via QR code first.");
                return false;
            }

            if (connectedUserPublicKey == null || connectedUserPublicKey.isEmpty()) {
                showErrorAndFinish("Connected user's key not found. Please scan QR code again.");
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
            adapter = new MessageAdapter(messageList, myUserId);
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
                    Message encryptedMessage = snapshot.getValue(Message.class);
                    if (encryptedMessage == null) {
                        Log.w(TAG, "Received null message from Firebase");
                        return;
                    }

                    String decryptedText;
                    try {
                        // Check if this message was sent by me
                        boolean isSentByMe = encryptedMessage.getSenderId() != null
                                && encryptedMessage.getSenderId().equals(myUserId);

                        if (isSentByMe) {
                            // I sent this message - decrypt using senderCopy (encrypted with MY public key)
                            if (encryptedMessage.getSenderCopy() != null && !encryptedMessage.getSenderCopy().isEmpty()) {
                                decryptedText = RSAEncryptionHelper.decrypt(
                                        ChatActivity.this,
                                        encryptedMessage.getSenderCopy()
                                );
                                Log.d(TAG, "✓ Decrypted my own message using senderCopy");
                            } else {
                                // Fallback for old messages without senderCopy
                                decryptedText = "[Your message - old format]";
                                Log.w(TAG, "SenderCopy not available for my message (old format)");
                            }
                        } else {
                            // They sent this message - decrypt using main text (encrypted with MY public key)
                            decryptedText = RSAEncryptionHelper.decrypt(
                                    ChatActivity.this,
                                    encryptedMessage.getText()
                            );
                            Log.d(TAG, "✓ Decrypted received message");
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Message decryption failed", e);
                        decryptedText = "[Could not decrypt message]";
                    }

                    Message decryptedMessage = new Message(
                            decryptedText,
                            encryptedMessage.getSenderId(),
                            encryptedMessage.getReceiverId(),
                            encryptedMessage.getTimestamp(),
                            encryptedMessage.getSenderPublicKey()
                    );

                    messageList.add(decryptedMessage);
                    if (adapter != null) {
                        adapter.notifyItemInserted(messageList.size() - 1);
                    }
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    updateEmptyState();

                    Log.d(TAG, "Message received and processed");

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

        if (plainText.length() > 500) {
            Toast.makeText(this, "Message too long. Maximum 500 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable send button temporarily
        if (buttonSend != null) {
            buttonSend.setEnabled(false);
        }

        try {
            Log.d(TAG, "Encrypting message for receiver...");
            // Encrypt with RECEIVER's public key (so they can decrypt with their private key)
            String encryptedForReceiver = RSAEncryptionHelper.encrypt(plainText, connectedUserPublicKey);

            Log.d(TAG, "Encrypting message for myself (sender copy)...");
            // Encrypt with MY public key (so I can decrypt with my private key)
            String encryptedForSender = RSAEncryptionHelper.encrypt(plainText, myPublicKey);

            // Create message with BOTH encrypted versions
            Message message = new Message(
                    encryptedForReceiver,    // text - encrypted for receiver
                    encryptedForSender,      // senderCopy - encrypted for sender
                    myUserId,
                    connectedUserId,
                    System.currentTimeMillis(),
                    myPublicKey
            );

            Log.d(TAG, "Sending message to Firebase...");
            if (messagesRef != null) {
                messagesRef.push().setValue(message).addOnCompleteListener(task -> {
                    // Re-enable send button
                    if (buttonSend != null) {
                        buttonSend.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        if (editTextMessage != null) {
                            editTextMessage.setText("");
                        }
                        Log.d(TAG, "✓ Message sent successfully (with sender copy)");
                    } else {
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "✗ Message send failed", task.getException());
                    }
                });
            }

        } catch (Exception e) {
            // Re-enable send button on error
            if (buttonSend != null) {
                buttonSend.setEnabled(true);
            }
            Log.e(TAG, "Message encryption/send error", e);
            Toast.makeText(this, "Failed to encrypt message: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getChatId(String user1, String user2) {
        if (user1 == null || user2 == null) {
            Log.w(TAG, "getChatId called with null user");
            return "unknown_chat";
        }
        String chatId = user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
        return chatId;
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
            Log.d(TAG, "Back button pressed");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Hardware back button pressed");
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
        Log.d(TAG, "ChatActivity destroyed");
    }
}