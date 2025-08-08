package com.tanjid.solynk;

public class Message {
    private String text;
    private String senderId;
    private String receiverId;
    private long timestamp;

    public Message() {
    }

    public Message(String text, String senderId, String receiverId) {
        this.text = text;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String text, String senderId, String receiverId, long timestamp) {
        this.text = text;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}