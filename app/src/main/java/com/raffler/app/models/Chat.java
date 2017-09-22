package com.raffler.app.models;

/**
 * Created by Ghost on 23/8/2017.
 */

public class Chat {
    private String title;
    private String userId;
    private String messageId;
    private long unreadCount;

    public Chat(String title, String userId, String messageId, long unreadCount) {
        this.title = title;
        this.userId = userId;
        this.messageId = messageId;
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }
}
