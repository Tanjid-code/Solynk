package com.tanjid.solynk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private Button buttonSend;

    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();

    private DatabaseReference messagesRef;
    private String myUserId, connectedUserId;
    private static final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        // Retrieve user IDs from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);
        myUserId = prefs.getString("myUserId", null);
        connectedUserId = prefs.getString("connectedUserId", null);

        Log.d(TAG, "onCreate: My User ID retrieved: " + myUserId);
        Log.d(TAG, "onCreate: Connected User ID retrieved: " + connectedUserId);

        if (myUserId == null || connectedUserId == null) {
            Toast.makeText(this, "Chat connection failed. Please re-connect.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "myUserId or connectedUserId is missing. Cannot start chat. Finishing activity.");
            finish();
            return;
        }

        Toast.makeText(this, "Chatting with: " + connectedUserId, Toast.LENGTH_SHORT).show();

        String chatPath = "chats/" + getChatId(myUserId, connectedUserId);
        Log.d(TAG, "onCreate: Calculated chat path: " + chatPath);

        messagesRef = FirebaseDatabase.getInstance().getReference(chatPath);

        adapter = new MessageAdapter(messageList, myUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Firebase ChildEventListener
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded: New message snapshot received. Key: " + snapshot.getKey());

                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    Log.d(TAG, "onChildAdded: Message object successfully parsed. Text: " + message.getText());
                    // Add message to list and update adapter
                    messageList.add(message);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                } else {
                    Log.w(TAG, "onChildAdded: Message object is null or failed to parse from snapshot.");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged: Message changed. Key: " + snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onChildRemoved: Message removed. Key: " + snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved: Message moved. Key: " + snapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase chat listener cancelled: " + error.getMessage() + " (Code: " + error.getCode() + ")");
                Toast.makeText(ChatActivity.this, "Failed to load messages: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Send button click listener
        buttonSend.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                Message message = new Message(text, myUserId, connectedUserId, System.currentTimeMillis());
                Log.d(TAG, "buttonSend: Attempting to send message. Text: '" + text + "', Sender: '" + myUserId + "', Receiver: '" + connectedUserId + "'");

                messagesRef.push().setValue(message, (databaseError, databaseReference) -> {
                    if (databaseError != null) {
                        Log.e(TAG, "Failed to send message to Firebase: " + databaseError.getMessage());
                        Toast.makeText(ChatActivity.this, "Error sending: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Log.d(TAG, "Message successfully pushed to Firebase. Path: " + databaseReference.getPath());
                        editTextMessage.setText(""); // Clear input only on success
                    }
                });
            } else {
                Log.d(TAG, "buttonSend: Message text is empty, not sending.");
            }
        });
    }

    private String getChatId(String user1, String user2) {
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }
}