package com.tanjid.solynk;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MessageAdapter";
    private List<Message> messageList;
    private String myUserId;
    private Context context;

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    public MessageAdapter(List<Message> messageList, String myUserId, Context context) {
        this.messageList = messageList;
        this.myUserId = myUserId;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(myUserId)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_MESSAGE_SENT) {
            ((SentMessageViewHolder) holder).bind(message, context);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message, context);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage, textViewTimestamp;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }

        void bind(Message message, Context context) {
            String displayText;

            // For SENT messages, decrypt using senderCopy (encrypted with MY public key)
            String encryptedText = message.getSenderCopy();

            if (encryptedText == null || encryptedText.isEmpty()) {
                // Fallback for old messages
                encryptedText = message.getText();
                Log.w(TAG, "SenderCopy is null, using text field");
            }

            // Decrypt the message
            try {
                displayText = RSAEncryptionHelper.decrypt(context, encryptedText);
                Log.d(TAG, "✓ Sent message decrypted successfully");
            } catch (Exception e) {
                displayText = "[Cannot decrypt message]";
                Log.e(TAG, "✗ Failed to decrypt sent message", e);
            }

            textViewMessage.setText(displayText);
            textViewTimestamp.setText(formatTime(message.getTimestamp()));
        }
    }

    private static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage, textViewTimestamp;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }

        void bind(Message message, Context context) {
            String displayText;

            // For RECEIVED messages, decrypt using text (encrypted with MY public key)
            String encryptedText = message.getText();

            // Decrypt the message
            try {
                displayText = RSAEncryptionHelper.decrypt(context, encryptedText);
                Log.d(TAG, "✓ Received message decrypted successfully");
            } catch (Exception e) {
                displayText = "[Cannot decrypt message]";
                Log.e(TAG, "✗ Failed to decrypt received message", e);
            }

            textViewMessage.setText(displayText);
            textViewTimestamp.setText(formatTime(message.getTimestamp()));
        }
    }

    private static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}