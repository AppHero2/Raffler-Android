package com.raffler.app.models;

import java.util.Map;

import static com.raffler.app.utils.Util.getStringFromData;

/**
 * Created by Ghost on 15/8/2017.
 */

public class User {

    private String idx, name, photo, phone, bio, pushToken;

    public User(Map<String, Object> data){
        this.idx = getStringFromData("uid", data);
        this.name = getStringFromData("name", data);
        this.photo = getStringFromData("photo", data);
        this.phone = getStringFromData("phone", data);
        this.bio = getStringFromData("bio", data);
        this.pushToken = getStringFromData("pushToken", data);
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
}
