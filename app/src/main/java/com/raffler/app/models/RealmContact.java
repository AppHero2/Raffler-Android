package com.raffler.app.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Ghost on 10/27/2017.
 */

public class RealmContact extends RealmObject{
    @PrimaryKey
    private String idx;
    private String name, phone;

    public void setIdx(String idx) {
        this.idx = idx;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
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
}
