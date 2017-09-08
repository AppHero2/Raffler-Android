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

    public Contact(String idx, String name, String phone){
        this.idx = idx;
        this.name = name;
        this.phone = phone;
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

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
