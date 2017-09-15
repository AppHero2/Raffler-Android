package com.raffler.app.models;

import java.util.Date;
import java.util.Map;

import static com.raffler.app.models.NewsType.LOSER;
import static com.raffler.app.utils.Util.getBooleanFromData;
import static com.raffler.app.utils.Util.getDateFromData;
import static com.raffler.app.utils.Util.getIntFromData;
import static com.raffler.app.utils.Util.getStringFromData;

/**
 * Created by Ghost on 9/14/2017.
 */

public class News {
    private String idx;
    private String title, content, relatedId;
    private NewsType type = LOSER;
    private boolean isRead = false;
    private Date createdAt, updatedAt;

    public News(Map<String, Object> data){
        updateData(data);
    }

    public void updateData(Map<String, Object> data){
        this.idx = getStringFromData("idx", data);
        this.title = getStringFromData("title", data);
        this.content = getStringFromData("content", data);
        this.relatedId = getStringFromData("raffleId", data);
        this.type = NewsType.values()[getIntFromData("newsType", data)];
        this.isRead = getBooleanFromData("isRead", data);
        this.createdAt = getDateFromData("createdAt", data);
        this.updatedAt = getDateFromData("updatedAt", data);
    }

    public String getIdx() {
        return idx;
    }

    public String getTitle() {
        return title;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public NewsType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public boolean isRead() {
        return isRead;
    }
}
