package com.raffler.app.models;

import java.util.Date;
import java.util.Map;

import static com.raffler.app.utils.Util.getBooleanFromData;
import static com.raffler.app.utils.Util.getDateFromData;
import static com.raffler.app.utils.Util.getIntFromData;
import static com.raffler.app.utils.Util.getMapDataFromData;
import static com.raffler.app.utils.Util.getStringFromData;

/**
 * Created by Ghost on 16/8/2017.
 */

public class Thread {

    public enum ThreadType {
        PERSION(0), GROUP(1), CHANNEL(2);

        private final int value;
        ThreadType(int value) {this.value = value;}
        public int getValue() {
            return value;
        }
    }

    private String idx, name, bio, thumb, image;
    private Date createdAt, updatedAt;
    private boolean hasNew = false, isPublic = false, isArchived = false;
    private User creator;
    private ThreadType type = ThreadType.PERSION;

    public Thread(Map<String, Object> data) {
        this.idx = getStringFromData("idx", data);
        this.name = getStringFromData("name", data);
        this.bio = getStringFromData("bio", data);
        this.thumb = getStringFromData("thumb", data);
        this.image = getStringFromData("image", data);
        this.createdAt = getDateFromData("createdAt", data);
        this.updatedAt = getDateFromData("updatedAt", data);
        this.hasNew = getBooleanFromData("hasNew", data);
        this.isPublic = getBooleanFromData("isPublic", data);
        this.isArchived = getBooleanFromData("isArchived", data);
        this.type = ThreadType.values()[getIntFromData("type", data)];

        Map<String, Object> creatorData = getMapDataFromData("creator", data);
        this.creator = new User(creatorData);
    }

    public String getIdx() {
        return idx;
    }

    public String getBio() {
        return bio;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getThumb() {
        return thumb;
    }

    public User getCreator() {
        return creator;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public boolean isHasNew() {
        return hasNew;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public ThreadType getType() {
        return type;
    }
}
