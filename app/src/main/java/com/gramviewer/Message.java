package com.gramviewer;

public class Message {
    public final String author;
    public final String text;
    public final boolean isSentByUser;

    public Message(String author, String text, boolean isSentByUser) {
        this.author = author;
        this.text = text;
        this.isSentByUser = isSentByUser;
    }
}