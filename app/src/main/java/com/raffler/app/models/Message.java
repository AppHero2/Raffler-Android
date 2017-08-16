package com.raffler.app.models;

import java.util.Map;

import static com.raffler.app.models.Message.MessageStatus.EDITING;
import static com.raffler.app.models.Message.MessageType.TEXT;
import static com.raffler.app.utils.Util.getBooleanFromData;
import static com.raffler.app.utils.Util.getIntFromData;
import static com.raffler.app.utils.Util.getStringFromData;

/**
 * Created by Ghost on 16/8/2017.
 */

public class Message {

    public enum MessageType {
        TEXT(0), PHOTO(1), AUDIO(2), VIDEO(3), LOCATION(4);

        private final int value;
        MessageType(int value) {this.value = value;}
        public int getValue() {
            return value;
        }
    }

    public enum MessageStatus {
        EDITING(0), SENDING(1), SENT(2), DELETED(3), FAILED(4);

        private final int value;
        MessageStatus(int value) {this.value = value;}
        public int getValue() {
            return value;
        }
    }

    private String idx, text, resource;
    private Boolean isDelivered = false, isRead = false;
    private MessageType type = TEXT;
    private MessageStatus status = EDITING;
    private String senderId, senderName;

    public Message(Map<String, Object> data){
        this.idx = getStringFromData("idx", data);
        this.text = getStringFromData("text", data);
        this.resource = getStringFromData("resource", data);
        this.isDelivered = getBooleanFromData("isDelivered", data);
        this.isRead = getBooleanFromData("isRead", data);
        this.senderId = getStringFromData("senderId", data);
        this.senderName = getStringFromData("senderName", data);
        this.type = MessageType.values()[getIntFromData("type", data)];
        this.status = MessageStatus.values()[getIntFromData("status", data)];
    }

    public String getIdx() {
        return idx;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public Boolean getDelivered() {
        return isDelivered;
    }

    public Boolean getRead() {
        return isRead;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public MessageType getType() {
        return type;
    }

    public String getResource() {
        return resource;
    }

    public String getText() {
        return text;
    }
}
