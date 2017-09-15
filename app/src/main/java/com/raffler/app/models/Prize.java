package com.raffler.app.models;

import com.raffler.app.utils.Util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ghost on 2/9/2017.
 */

public class Prize {

    private String idx, title, description, imageLink;
    private Date createdAt, updatedAt;
    private boolean isDelivered = false;

    public Prize(Map<String, Object> data){
        updateValue(data);
    }

    public void updateValue(Map<String, Object> data) {
        this.idx = Util.getStringFromData("idx", data);
        this.title = Util.getStringFromData("title", data);
        this.description = Util.getStringFromData("description", data);
        this.imageLink = Util.getStringFromData("imageLink", data);
        this.isDelivered = Util.getBooleanFromData("isDelivered", data);
        long createdAt = Util.getLongFromData("createdAt", data);
        this.createdAt = new Date(createdAt);
        long updatedAt = Util.getLongFromData("updatedAt", data);
        this.updatedAt = new Date(updatedAt);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIdx() {
        return idx;
    }

    public String getImageLink() {
        return imageLink;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
