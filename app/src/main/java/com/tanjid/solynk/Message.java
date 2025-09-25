package com.tanjid.solynk;

public class Message {
    private String text;                // Encrypted for receiver
    private String senderCopy;          // Encrypted for sender (NEW FIELD)
    private String senderId;
    private String receiverId;
    private long timestamp;
    private String senderPublicKey;

    // Default constructor required for Firebase
    public Message() {
    }

    // New constructor with senderCopy
    public Message(String text, String senderCopy, String senderId, String receiverId, long timestamp, String senderPublicKey) {
        this.text = text;
        this.senderCopy = senderCopy;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.senderPublicKey = senderPublicKey;
    }

    // Old constructor for backward compatibility
    public Message(String text, String senderId, String receiverId, long timestamp, String senderPublicKey) {
        this.text = text;
        this.senderCopy = null;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.senderPublicKey = senderPublicKey;
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderCopy() {
        return senderCopy;
    }

    public void setSenderCopy(String senderCopy) {
        this.senderCopy = senderCopy;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderPublicKey() {
        return senderPublicKey;
    }

    public void setSenderPublicKey(String senderPublicKey) {
        this.senderPublicKey = senderPublicKey;
    }
}