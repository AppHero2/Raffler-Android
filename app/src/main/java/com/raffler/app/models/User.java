package com.raffler.app.models;

import java.util.Date;
import java.util.Map;

import static com.raffler.app.models.UserAction.IDLE;
import static com.raffler.app.models.UserStatus.OFFLINE;
import static com.raffler.app.utils.Util.getBooleanFromData;
import static com.raffler.app.utils.Util.getDateFromData;
import static com.raffler.app.utils.Util.getIntFromData;
import static com.raffler.app.utils.Util.getStringFromData;

/**
 * Created by Ghost on 15/8/2017.
 */

public class User {

    private String idx, name, photo, phone, bio, pushToken;
    private UserStatus userStatus = OFFLINE;
    private UserAction userAction = IDLE;
    private Date lastOnlinedAt, lastUpdatedAt;

    public User(Map<String, Object> data){
        this.idx = getStringFromData("uid", data);
        this.name = getStringFromData("name", data);
        this.photo = getStringFromData("photo", data);
        this.phone = getStringFromData("phone", data);
        this.bio = getStringFromData("bio", data);
        this.pushToken = getStringFromData("pushToken", data);
        this.userStatus = UserStatus.values()[getIntFromData("userStatus", data)];
        this.userAction = userAction.values()[getIntFromData("userAction", data)];
        this.lastOnlinedAt = getDateFromData("lastOnlinedAt", data);
        this.lastUpdatedAt = getDateFromData("lastUpdatedAt", data);
    }

    public String getBio() {
        return bio;
    }

    public String getIdx() {
        return idx;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getPhoto() {
        return photo;
    }

    public String getPushToken() {
        return pushToken;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public UserAction getUserAction() {
        return userAction;
    }

    public Date getLastOnlinedAt() {
        return lastOnlinedAt;
    }

    public Date getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
