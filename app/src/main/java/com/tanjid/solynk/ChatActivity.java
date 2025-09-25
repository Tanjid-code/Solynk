package com.tanjid.solynk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // Assuming you're still using UUID for message IDs

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private Button buttonSend;

    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();

    private DatabaseReference messagesRef;
    private String myUserId;
    private String connectedUserId; // Store this explicitly
    private static final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        // Load current user's ID and the ID of the user they are connected to
        SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);
        myUserId = prefs.getString("myUserId", null);
        connectedUserId = prefs.getString("connectedUserId", null);

        // Crucial check: Ensure both IDs are available
        if (myUserId == null || connectedUserId == null || "ID_NOT_FOUND".equals(myUserId) || "ID_NOT_FOUND".equals(connectedUserId)) {
            Toast.makeText(this, "Chat connection not established. Please ensure both users have scanned each other's QR codes and IDs are saved.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Chat initialization failed: myUserId=" + myUserId + ", connectedUserId=" + connectedUserId);
            finish(); // Close activity if IDs are missing
            return;
        }

        // Set toolbar title dynamically
        if (getSupportActionBar() != null) {
            String displayConnectedId = connectedUserId.length() > 8 ? connectedUserId.substring(0, 8) + "..." : connectedUserId;
            getSupportActionBar().setTitle("Chat with " + displayConnectedId);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Generate a consistent chat path for both users
        String chatPath = "chats/" + getChatId(myUserId, connectedUserId);
        messagesRef = FirebaseDatabase.getInstance().getReference(chatPath);
        Log.d(TAG, "Firebase Chat Path: " + chatPath); // Log the path to verify

        adapter = new MessageAdapter(messageList, myUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Attach listener for new messages
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message encryptedMessage = snapshot.getValue(Message.class);
                if (encryptedMessage != null) {
                    try {
                        String decryptedText = EncryptionHelper.decrypt(encryptedMessage.getText());
                        Message decryptedMessage = new Message(
                                decryptedText,
                                encryptedMessage.getSenderId(),
                                encryptedMessage.getReceiverId(),
                                encryptedMessage.getTimestamp()
                        );
                        messageList.add(decryptedMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    } catch (Exception e) { // Catch decryption errors
                        Log.e(TAG, "Decryption failed for message: " + encryptedMessage.getText(), e);
                        // Optionally, add a placeholder message or skip
                    }
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { /* Not strictly needed for simple chat */ }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) { /* Not strictly needed for simple chat */ }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { /* Not strictly needed for simple chat */ }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled: " + error.getMessage() + ", Details: " + error.getDetails());
                Toast.makeText(ChatActivity.this, "Failed to load messages: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        buttonSend.setOnClickListener(v -> {
            String plainText = editTextMessage.getText().toString().trim();
            if (!plainText.isEmpty()) {
                try {
                    String encryptedText = EncryptionHelper.encrypt(plainText);
                    // Use the correct myUserId and connectedUserId
                    Message message = new Message(encryptedText, myUserId, connectedUserId, System.currentTimeMillis());
                    // Push the message to the database
                    messagesRef.push().setValue(message).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            editTextMessage.setText(""); // Clear input field
                        } else {
                            Toast.makeText(ChatActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to send message: ", task.getException());
                        }
                    });
                } catch (Exception e) { // Catch encryption errors
                    Log.e(TAG, "Encryption failed for message: " + plainText, e);
                    Toast.makeText(ChatActivity.this, "Failed to encrypt message.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ChatActivity.this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Generates a unique chat ID by sorting the two user IDs lexicographically.
     * This ensures both users will arrive at the same chat path in Firebase.
     */
    private String getChatId(String user1, String user2) {
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}