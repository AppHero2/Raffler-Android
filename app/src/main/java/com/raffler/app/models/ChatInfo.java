package com.raffler.app.models;

import com.raffler.app.utils.Util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ghost on 23/8/2017.
 */

public class ChatInfo {

    private String idx;
    private Map<String, Object> phones = new HashMap<>();
    private Map<String, Object> photos = new HashMap<>();
    private long unreadCount;
    private Date updatedAt;
    private String lastMessage, lastMessageId, lastSender;

    public ChatInfo(String idx) {
        this.idx = idx;
    }

    public ChatInfo(String idx, Map<String, Object> data) {
        this.idx = idx;
        this.updateValue(data);
    }

    public void updateValue(Map<String, Object> data){
        this.phones = Util.getMapDataFromData("phones", data);
        this.photos = Util.getMapDataFromData("photos", data);
        this.unreadCount = Util.getLongFromData("unreadCount", data);
        this.updatedAt = Util.getDateFromData("updatedAt", data);
        this.lastMessage = Util.getStringFromData("lastMessage", data);
        this.lastMessageId = Util.getStringFromData("lastMessageId", data);
        this.lastSender = Util.getStringFromData("lastSender", data);
    }

    public String getIdx() {
        return idx;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, Object> getPhones() {
        return phones;
    }

    public Map<String, Object> getPhotos() {
        return photos;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public String getLastSender() {
        return lastSender;
    }
}
