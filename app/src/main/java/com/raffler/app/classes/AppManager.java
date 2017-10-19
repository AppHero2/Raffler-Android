package com.raffler.app.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.raffler.app.country.Country;
import com.raffler.app.interfaces.NewsValueListener;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.Contact;
import com.raffler.app.models.News;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppManager {

    private static final String TAG = "AppManager";

    private static final AppManager ourInstance = new AppManager();

    public static AppManager getInstance() {
        return ourInstance;
    }

    private Context context;
    private Country country;
    private ValueEventListener trackUserListener;
    private ChildEventListener trackNewsListener;
    private UserValueListener userValueListenerMain, userValueListenerForChat, userValueListenerForRaffles;
    private NewsValueListener newsValueListenerMain, newsValueListenerForNews;

    public Chat selectedChat;
    public boolean loadedPhoneContacts = false;
    public Map<String, String> phoneContacts = new HashMap<>();
    public List<News> newsList = new ArrayList<>();

    private AppManager() {

    }

    /**
     * this method is used to track user
     * @param uid : user identity
     */
    public void startTrackingUser(String uid) {
        if (trackUserListener != null)
            return;
        trackUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null){
                    Map<String, Object> userData = (Map<String, Object>) dataSnapshot.getValue();
                    User user = new User(userData);
                    AppManager.saveSession(user);
                    if (userValueListenerMain != null) {
                        userValueListenerMain.onLoadedUser(user);
                    }
                    if (userValueListenerForChat != null) {
                        userValueListenerForChat.onLoadedUser(user);
                    }
                    if (userValueListenerForRaffles != null) {
                        userValueListenerForRaffles.onLoadedUser(user);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("TrackUser", databaseError.toString());
            }
        };
        References.getInstance().usersRef.child(uid).addValueEventListener(trackUserListener);
    }

    public void stopTrackingUser(String uid){
        if (trackUserListener != null)
            References.getInstance().usersRef.child(uid).removeEventListener(trackUserListener);
    }

    /**
     * this method is used to track news for a user
     * @param uid : user identity
     */
    public void startTrackingNews(String uid){
        if (trackNewsListener != null)
            return;

        trackNewsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> newsData = (Map<String, Object>) dataSnapshot.getValue();
                    updateNewsData(newsData);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> newsData = (Map<String, Object>) dataSnapshot.getValue();
                    updateNewsData(newsData);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> newsData = (Map<String, Object>) dataSnapshot.getValue();
                    News raffle = new News(newsData);
                    for (News item : newsList) {
                        if (item.getIdx().equals(raffle.getIdx())) {
                            newsList.remove(item);
                            break;
                        }
                    }

                    if (newsValueListenerMain != null) {
                        newsValueListenerMain.onUpdatedNewsList(newsList);
                    }

                    if (newsValueListenerForNews != null) {
                        newsValueListenerForNews.onUpdatedNewsList(newsList);
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        };

        References.getInstance().newsRef.child(uid).addChildEventListener(trackNewsListener);
    }

    /**
     * this method is used to update existing news
     * @param data : updated data
     */
    private void updateNewsData(Map<String, Object> data){
        News news = new News(data);
        if (news.getIdx() == null)
            return;

        boolean isExist = false;
        for (News item : newsList) {
            if (item.getIdx().equals(news.getIdx())) {
                item.updateData(data);
                isExist = true;
                break;
            }
        }

        if (!isExist) {
            newsList.add(news);
        }

        if (newsValueListenerMain != null) {
            newsValueListenerMain.onUpdatedNewsList(newsList);
        }

        if (newsValueListenerForNews != null) {
            newsValueListenerForNews.onUpdatedNewsList(newsList);
        }
    }

    /**
     * This function is used to get user data from firebase database
     * @param userId : registered user id to get
     * @param listener : callback to get event
     */
    public static void getUser(String userId, final UserValueListener listener) {
        if (userId != null) {
            Query query = References.getInstance().usersRef.child(userId);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null){
                        Map<String, Object> userData = (Map<String, Object>) dataSnapshot.getValue();
                        User user = new User(userData);
                        if (listener != null) {
                            listener.onLoadedUser(user);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("TrackUser", databaseError.toString());
                }
            });
        } else {
            if (listener != null) {
                listener.onLoadedUser(null);
            }
        }
    }

    /**
     * save user data to local storage
     * @param user
     */
    public static void saveSession(User user){
        Context context = AppManager.getInstance().context;
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uid", user.getIdx());
        editor.putString("bio", user.getBio());
        editor.putString("name", user.getName());
        editor.putString("photo", user.getPhoto());
        editor.putString("phone", user.getPhone());
        editor.putString("pushToken", user.getPushToken());
        editor.putInt("userStatus", user.getUserStatus().ordinal());
        editor.putInt("userAction", user.getUserAction().ordinal());
        Gson gson = new Gson();
        String chatsDic = gson.toJson(user.getChats());
        editor.putString("chats", chatsDic);
        String lastSeen = gson.toJson(user.getLastseens());
        editor.putString("lastseens", lastSeen);
        String raffles = gson.toJson(user.getRaffles());
        editor.putString("raffles", raffles);
        editor.putInt("raffle_point", user.getRaffle_point());
        editor.commit();
    }

    /**
     * get user data from local storage
     * @return user
     */
    public static User getSession() {
        Context context = AppManager.getInstance().context;
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE);
        String uid = sharedPreferences.getString("uid", null);
        String name = sharedPreferences.getString("name", "?");
        String photo = sharedPreferences.getString("photo", "?");
        String phone = sharedPreferences.getString("phone", "");
        String bio = sharedPreferences.getString("bio", "?");
        String pushToken = sharedPreferences.getString("pushToken", "?");
        int userStatus = sharedPreferences.getInt("userStatus", 0);
        int userAction = sharedPreferences.getInt("userAction", 0);
        int raffle_point = sharedPreferences.getInt("raffle_point", 0);
        String chatsDic = sharedPreferences.getString("chats", null);
        Map<String,Object> chats = new Gson().fromJson(chatsDic, new TypeToken<Map<String, Object>>(){}.getType());
        String lastSeenDic = sharedPreferences.getString("lastseens", null);
        Map<String,Object> lastSeens = new Gson().fromJson(lastSeenDic, new TypeToken<Map<String, Object>>(){}.getType());
        String rafflesDic = sharedPreferences.getString("raffles", null);
        Map<String,Object> raffles = new Gson().fromJson(rafflesDic, new TypeToken<Map<String, Object>>(){}.getType());
        if (uid != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("name", name);
            data.put("photo", photo);
            data.put("phone", phone);
            data.put("bio", bio);
            data.put("pushToken", pushToken);
            data.put("userStatus", userStatus);
            data.put("userAction", userAction);
            data.put("chats", (chats != null) ? chats : new HashMap<String, Object>());
            data.put("lastseens", (lastSeens != null) ? lastSeens : new HashMap<String, Object>());
            data.put("raffles", (raffles != null) ? raffles : new HashMap<String, Object>());
            data.put("raffle_point", raffle_point);
            User user = new User(data);
            return user;
        } else {
            return null;
        }
    }

    /**
     * delete user data from local storage
     * this method might be used when user logout
     * @param context
     */
    public static void deleteSession(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uid", null);
        editor.commit();
    }

    /**
     * save contacts data into local storage
     * @param contacts
     */
    public static void saveContacts(Map<String,String> contacts){
        Context context = AppManager.getInstance().context;
        SharedPreferences sharedPreferences = context.getSharedPreferences("Contacts", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String contactsDic = gson.toJson(contacts);
        editor.putString("contacts", contactsDic);
        editor.commit();
    }

    /**
     * get saved contacts data from local storage
     * @return dictionary format
     */
    public static Map<String, String> getContacts(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("Contacts", Context.MODE_PRIVATE);
        String contactsDic = sharedPreferences.getString("contacts", null);
        Map<String,String> contacts = new Gson().fromJson(contactsDic, new TypeToken<Map<String, String>>(){}.getType());
        if (contacts == null)
            contacts = new HashMap<>();
        return contacts;
    }

    /**
     * load all contacts from Phone Contact
     * this method needs async task because it may take too longer to load
     * @param listener : success result handler
     */
    public void loadPhoneContacts(ResultListener listener){
        long startnow = android.os.SystemClock.uptimeMillis();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[] {
                Phone._ID,
                Phone.DISPLAY_NAME,
                Phone.NUMBER
        };

        Cursor people = context.getContentResolver().query(uri, projection, null, null, null);
        int indexId = people.getColumnIndex(Phone._ID);
        int indexName = people.getColumnIndex(Phone.DISPLAY_NAME);
        int indexNumber = people.getColumnIndex(Phone.NUMBER);

        if(people.moveToFirst()) {
            do {
                String idx = people.getString(indexId);
                String name   = people.getString(indexName);
                String number = people.getString(indexNumber);
                // Do work...
                String phone = number.replace(" ", "").replace("-", "");
                Contact contact = new Contact(idx, name, phone);
                boolean isExist = false;
                for (Map.Entry<String, String> entry : phoneContacts.entrySet()){
                    if (entry.getKey().equals(contact.getIdx())){
                        isExist = true;
                        break;
                    }
                }
                if (!isExist)
                    phoneContacts.put(phone, name);

                saveContacts(phoneContacts);

            } while (people.moveToNext());
        }

        long endnow = android.os.SystemClock.uptimeMillis();
        long processingDuration = endnow - startnow;
        Log.d("AppManager", "TimeForContacts " + (endnow - startnow) + " ms");

        // analysis
        Bundle params = new Bundle();
        params.putLong("duration", processingDuration);
        References.getInstance().analytics.logEvent("load_contacts", params);

        if (listener != null) {
            listener.onResult(true);
        }
    }

    /**
     * this method is used to find contacts with a phone number
     * @param phone : needs to find
     * @return contacts
     */
    public Contact getPhoneContact(String phone, String countryCode) {

        if (phoneContacts.size() == 0) phoneContacts = getContacts(context);

        Contact existing_contact = null;
        for (Map.Entry<String, String> entry : phoneContacts.entrySet()) {
            String contactPhone = entry.getKey();
            String contactName = entry.getValue();
            if (!contactPhone.contains("+")){
                String regionCode = country.getDialCode();
                contactPhone = regionCode + contactPhone;
                if (phone.contains(regionCode)){
                    String nationalPhoneNumber = phone.replace(regionCode, "");
                    if (contactPhone.contains(nationalPhoneNumber)) {
                        existing_contact = new Contact(null, contactName, contactPhone);
                        break;
                    }
                }
            } else {
                if (contactPhone.contains(countryCode)){
                    String nationalPhoneNumber = phone.replace(countryCode, "");
                    if (contactPhone.contains(nationalPhoneNumber)) {
                        existing_contact = new Contact(null, contactName, contactPhone);
                        break;
                    }
                }
            }
        }
        return existing_contact;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public void setUserValueListenerMain(UserValueListener userValueListenerMain) {
        this.userValueListenerMain = userValueListenerMain;
    }

    public void setUserValueListenerForChat(UserValueListener userValueListenerForChat) {
        this.userValueListenerForChat = userValueListenerForChat;
    }

    public void setUserValueListenerForRaffles(UserValueListener userValueListenerForRaffles) {
        this.userValueListenerForRaffles = userValueListenerForRaffles;
    }

    public void setNewsValueListenerMain(NewsValueListener newsValueListenerMain) {
        this.newsValueListenerMain = newsValueListenerMain;
    }

    public void setNewsValueListenerForNews(NewsValueListener newsValueListenerForNews) {
        this.newsValueListenerForNews = newsValueListenerForNews;
    }
}
