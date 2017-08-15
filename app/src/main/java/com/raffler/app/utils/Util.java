package com.raffler.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Util {

    public static void requestPermission(Activity activity, String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, 0);
        }
    }

    public static String formatSeconds(int seconds) {
        return getTwoDecimalsValue(seconds / 3600) + ":"
                + getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60);
    }

    private static String getTwoDecimalsValue(int value) {
        if (value >= 0 && value <= 9) {
            return "0" + value;
        } else {
            return value + "";
        }
    }

    public static String getUnicodeString(String myString) {
        String text = "";
        try {

            byte[] utf8Bytes = myString.getBytes("UTF8");
            text = new String(utf8Bytes, "UTF8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return text;
    }

    public static String escapeUnicodeText(String input) {

        StringBuilder b = new StringBuilder(input.length());

        java.util.Formatter f = new java.util.Formatter(b);

        for (char c : input.toCharArray()) {
            if (c < 128) {
                b.append(c);
            } else {
                f.format("\\u%04x", (int) c);
            }
        }

        return b.toString();
    }

    /**
     * get values from Hash map data
     */
    public static Date getDateFromData(String key, Map<String, Object> data){
        Date date = new Date();
        String strDate = null;
        long timeSince1970 = System.currentTimeMillis();
        try{
            timeSince1970 = (long) data.get(key);
            date = new Date(timeSince1970);
        }catch (Exception e){
            e.printStackTrace();
        }

        if (date == null) date = new Date();

        return date;
    }

    public static int getIntFromData(String key, Map<String, Object> data){
        Integer value = 0;
        try{
            String strValue = data.get(key).toString();
            value = Integer.parseInt(strValue);
        }catch (Exception e){
            e.printStackTrace();
        }
        return  value;
    }

    public static String getStringFromData(String key, Map<String, Object> data){
        String value = "?";
        try{
            value = (String) data.get(key);
        }catch (Exception e){
            e.printStackTrace();
        }
        return  value;
    }

    public static boolean getBooleanFromData(String key, Map<String, Object> data){
        boolean value = false;
        try{
            value = (boolean) data.get(key);
        }catch (Exception e){
            e.printStackTrace();
        }
        return  value;
    }

    public static Map<String, String> getListDataFromData(String key, Map<String, Object> data){
        Map<String, String> value = new HashMap<>();
        try{
            value = (Map<String, String>) data.get(key);
        }catch (Exception e){
            e.printStackTrace();
        }
        if (value == null) value = new HashMap<>();
        return  value;
    }

    /**
     *  Handler
     */

    private static final Handler HANDLER = new Handler();
    public static void wait(int millis, Runnable callback){
        HANDLER.postDelayed(callback, millis);
    }

    private static String PREF_HIGH_QUALITY = "pref_high_quality";

    public static void setPrefHighQuality(Context context, boolean isEnabled) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_HIGH_QUALITY, isEnabled);
        editor.apply();
    }

    public static boolean getPrefHighQuality(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREF_HIGH_QUALITY, false);
    }
}