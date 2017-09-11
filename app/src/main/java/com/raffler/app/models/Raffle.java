package com.raffler.app.models;

import com.raffler.app.utils.Util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ghost on 2/9/2017.
 */

public class Raffle {

    private String idx, description, imageLink;
    private Date endingAt;
    private boolean isClosed = false;
    private long raffles_num = 0;
    private long winners_num = 0;

    private Map<String, Object> rafflers = new HashMap<>();
    private Map<String, Object> winners = new HashMap<>();
    private Map<String, Object> deliveredUsers = new HashMap<>();

    public Raffle(Map<String, Object> data){
        updateValue(data);
    }

    public void updateValue(Map<String, Object> data) {
        this.idx = Util.getStringFromData("idx", data);
        this.description = Util.getStringFromData("description", data);
        this.imageLink = Util.getStringFromData("imageLink", data);
        this.raffles_num = Util.getLongFromData("raffles_num", data);
        this.winners_num = Util.getLongFromData("winners_num", data);
        this.isClosed = Util.getBooleanFromData("isClosed", data);
        long ending_date =  Util.getLongFromData("ending_date", data);
        this.endingAt = new Date(ending_date);
        this.rafflers = Util.getMapDataFromData("rafflers", data);
        this.winners = Util.getMapDataFromData("winners", data);
        this.deliveredUsers = Util.getMapDataFromData("deliveredUsers", data);
    }

    public Date getEndingAt() {
        return endingAt;
    }

    public long getRaffles_num() {
        return raffles_num;
    }

    public long getWinners_num() {
        return winners_num;
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

    public Map<String, Object> getRafflers() {
        return rafflers;
    }

    public Map<String, Object> getWinners() {
        return winners;
    }

    public Map<String, Object> getDeliveredUsers() {
        return deliveredUsers;
    }

    public boolean isExistRaffler(String uid){
        boolean isExist = false;
        for (Map.Entry<String, Object> entry : rafflers.entrySet()){
            String rafflerId = entry.getKey();
            if (rafflerId.equals(uid)) {
                isExist = true;
                break;
            }
        }
        return isExist;
    }

    public boolean isExistWinner(String uid){
        boolean isExist = false;
        for (Map.Entry<String, Object> entry : winners.entrySet()){
            String rafflerId = entry.getKey();
            if (rafflerId.equals(uid)) {
                isExist = true;
                break;
            }
        }
        return isExist;
    }

    public boolean isExistDelivered(String uid){
        boolean isExist = false;
        for (Map.Entry<String, Object> entry : deliveredUsers.entrySet()){
            String rafflerId = entry.getKey();
            if (rafflerId.equals(uid)) {
                isExist = true;
                break;
            }
        }
        return isExist;
    }
}
