package com.raffler.app.models;


import android.net.Uri;

import java.io.File;
import java.util.Date;
import java.util.Map;

import static com.raffler.app.models.ChatType.PERSONAL;
import static com.raffler.app.models.MessageStatus.SENDING;
import static com.raffler.app.models.MessageType.TEXT;
import static com.raffler.app.models.UserType.OTHER;
import static com.raffler.app.models.UserType.SELF;
import static com.raffler.app.utils.Util.getDateFromData;
import static com.raffler.app.utils.Util.getIntFromData;
import static com.raffler.app.utils.Util.getStringFromData;

/**
 * Created by Ghost on 16/8/2017.
 */

public class Message {

    private String idx, uid, text, resource;
    private ChatType chatType = PERSONAL;
    private MessageType messageType = TEXT;
    private MessageStatus status = SENDING;
    private UserType userType = SELF;
    private String senderId, senderName, senderPhoto;
    private Date createdAt, updatedAt;
    private Uri attachFilePath;

    public Message(String userId, Map<String, Object> data){
        this.uid = userId;
        this.updateValue(data);
    }

    public void updateValue(Map<String, Object> data){
        this.idx = getStringFromData("idx", data);
        this.text = getStringFromData("text", data);
        this.resource = getStringFromData("resource", data);
        this.senderId = getStringFromData("senderId", data);
        this.senderName = getStringFromData("senderName", data);
        this.senderPhoto = getStringFromData("senderPhoto", data);
        this.chatType = ChatType.values()[getIntFromData("chatType", data)];
        this.messageType = MessageType.values()[getIntFromData("messageType", data)];
        this.status = MessageStatus.values()[getIntFromData("status", data)];
        if (this.uid != null && this.senderId != null)
            this.userType = senderId.equals(this.uid) ? SELF : OTHER;
        this.createdAt = getDateFromData("createdAt", data);
        this.updatedAt = getDateFromData("updatedAt", data);
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

    public String getSenderPhoto() {
        return senderPhoto;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public ChatType getChatType() {
        return chatType;
    }

    public String getResource() {
        return resource;
    }

    public String getText() {
        return text;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setAttachFilePath(Uri attachFilePath) {
        this.attachFilePath = attachFilePath;
    }

    public Uri getAttachFilePath() {
        return attachFilePath;
    }
}
