package com.raffler.app.interfaces;

/**
 * Created by Ghost on 23/8/2017.
 */

public interface UnreadMessageListener {
    void onUnreadMessages(String chatId, int count);
}
