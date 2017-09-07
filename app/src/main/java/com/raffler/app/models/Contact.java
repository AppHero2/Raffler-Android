package com.raffler.app.models;

/**
 * Created by Ghost on 9/8/2017.
 */

public class Contact {

    private String idx, name, phone, uid, photo;

    public Contact(String idx, String name, String phone, String uid, String photo){
        this.idx = idx;
        this.name = name;
        this.phone = phone;
        this.uid = uid;
        this.photo = photo;
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

    public String getUid() {
        return uid;
    }
}
