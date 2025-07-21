package com.example.fithit;
public class Message {
    public String sender;
    public String message;
    public long timestamp;

    public Message() {} // Required for Firebase

    public Message(String sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }
}

