package com.raffler.app.models;

/**
 * Created by Ghost on 23/8/2017.
 */

public class Chat {
    private User user;
    private String messageId;
    private long unreadCount;

    public Chat(User user, String messageId, long unreadCount) {
        this.user = user;
        this.messageId = messageId;
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public String getMessageId() {
        return messageId;
    }

    public User getUser() {
        return user;
    }
}
