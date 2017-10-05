package com.raffler.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.CircleBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.raffler.app.R;
import com.raffler.app.alertView.AlertView;
import com.raffler.app.classes.AppConsts;
import com.raffler.app.classes.AppManager;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Util {

    public static DisplayImageOptions displayImageOptions_original = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.img_logo)
            .showImageForEmptyUri(R.drawable.img_logo)
            .showImageOnFail(R.drawable.img_logo)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .build();

    public static DisplayImageOptions displayImageOptions_circluar = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.ic_profile_person)
            .showImageForEmptyUri(R.drawable.ic_profile_person)
            .showImageOnFail(R.drawable.ic_profile_person)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .displayer(new CircleBitmapDisplayer(0xccff8000, 1))
            .build();

    public static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {

        static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }

    public static void setProfileImage(String url, ImageView imgView){
        if (url != null) {
            ImageLoader.getInstance().displayImage(url, imgView, Util.displayImageOptions_circluar, new Util.AnimateFirstDisplayListener());
        }
    }

    public static void setProfileImage(String url, ImageView imgView, ImageLoadingListener listener) {
        if (url != null) {
            if (ImageLoader.getInstance() != null) {
                ImageLoader.getInstance().displayImage(url, imgView, Util.displayImageOptions_circluar, listener);
            }
        }
    }

    public static void setURLImage(String url, ImageView imgView){
        if (url != null) {
            ImageLoader.getInstance().displayImage(url, imgView, Util.displayImageOptions_original, new Util.AnimateFirstDisplayListener());
        }
    }

    public static void setURLImage(String url, ImageView imgView, ImageLoadingListener listener) {
        if (url != null) {
            if (ImageLoader.getInstance() != null) {
                ImageLoader.getInstance().displayImage(url, imgView, Util.displayImageOptions_original, listener);
            }
        }
    }

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

    public static String getMessageTime(Date time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        return simpleDateFormat.format(time);
    }

    public static String getUserTime(long datetime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(datetime);
        return getUserTime(calendar);
    }

    public static String getUserTime(Calendar calendar) {
        return getUserTime(calendar.getTime());
    }

    public static String getUserTime(Date time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        return simpleDateFormat.format(time);
    }

    public static String getUserFriendlyDate(Context context, long millis) {

        millis = getMillisForDateOnly(millis);
        long currentMillis = getMillisForDateOnly(System.currentTimeMillis());

        long diff = currentMillis - millis;
        int days = (int) Math.floor(diff / (1000 * 60 * 60 * 24));
        if (days == 1)
            return context.getResources().getString(R.string.yesterday);
        else {
            return Util.getSimpleDateString(millis);
        }
    }

    public static String getUserFriendlyDateForChat(Context context, long millis) {

        millis = getMillisForDateOnly(millis);
        long currentMillis = getMillisForDateOnly(System.currentTimeMillis());

        long diff = currentMillis - millis;
        int days = (int) Math.floor(diff / (1000 * 60 * 60 * 24));
        if (days == 0)
            return context.getResources().getString(R.string.today);
        else if (days == 1)
            return context.getResources().getString(R.string.yesterday);
        else {
            return Util.getChatDateString(millis);
        }
    }

    public static String getSimpleDateString(long millis) {

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        return getSimpleDateString(date.getTime());

    }

    public static String getSimpleDateString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());

        return format.format(date);
    }

    public static String getChatDateString(long millis) {

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        return getChatDateString(date.getTime());

    }

    public static String getChatDateString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        return format.format(date);
    }

    private static long getMillisForDateOnly(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static String getDateString(long millis) {

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        return getDateString(date.getTime());

    }

    public static String getDateString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        return format.format(date);
    }

    public static String generateChatKeyFrom(String publisher, String subscriber) {

        return (publisher.compareTo(subscriber) < 0 ? publisher + "_" + subscriber
                : subscriber + "_" + publisher);
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
        if(value == null) value = 0;
        return value;
    }

    public static long getLongFromData(String key, Map<String, Object> data){
        long value = 0;
        try{
            value = (long)data.get(key);
        }catch (Exception e){
            e.printStackTrace();
        }
        return value;
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

    public static Map<String, Object> getMapDataFromData(String key, Map<String, Object> data){
        Map<String, Object> value = new HashMap<>();
        try{
            value = (Map<String, Object>) data.get(key);
        }catch (Exception e){
            e.printStackTrace();
        }
        if (value == null) value = new HashMap<>();
        return  value;
    }

    public static String[] getUserIdsFrom(String chatId) {
        String[] userIds = chatId.split("_");
        return userIds;
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

    public static void showAlert (String title, String message, Context context) {
        AlertView alertView = new AlertView(title, message, context.getString(R.string.alert_button_okay), null, null, context, AlertView.Style.Alert, null);
        alertView.show();
    }

}