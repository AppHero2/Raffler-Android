package com.raffler.app.models;

/**
 * Created by Ghost on 23/8/2017.
 */

public class Chat {
    private User user;
    private Message message;
    private int unreadCount;

    public Chat(User user, Message message, int unreadCount) {
        this.user = user;
        this.message = message;
        this.unreadCount = unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public Message getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }
}
