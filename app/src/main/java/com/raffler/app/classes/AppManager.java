package com.raffler.app.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.interfaces.UnreadMessageListener;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.Contact;
import com.raffler.app.models.Message;
import com.raffler.app.models.MessageStatus;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;

import java.util.HashMap;
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
    private DatabaseReference usersRef;
    private ValueEventListener trackUserListener;
    private UserValueListener userValueListener, userValueListenerForChat;

    public Chat selectedChat;
    public String userId;
    public Map<String, String> phoneContacts = new HashMap<>();

    private AppManager() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
        }
        usersRef = database.getReference("Users");
    }

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
                    if (userValueListener != null) {
                        userValueListener.onLoadedUser(user);
                    }
                    if (userValueListenerForChat != null) {
                        userValueListenerForChat.onLoadedUser(user);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("TrackUser", databaseError.toString());
            }
        };
        usersRef.child(uid).addValueEventListener(trackUserListener);
    }

    public void stopTrackingUser(String uid){
        usersRef.child(uid).removeEventListener(trackUserListener);
    }

    public static void getUser(String userId, final UserValueListener listener) {
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
    }

    public static void getUnreadMessageCount(final String chatId, final UnreadMessageListener listener){

        Query queryUnreadMessages = References.getInstance().messagesRef.child(chatId).orderByChild("status").equalTo(MessageStatus.DELIVERED.ordinal());
        queryUnreadMessages.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    int unread_count = 0;
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Map<String, Object> messageData = (Map<String, Object>) child.getValue();
                        Message message = new Message(messageData);
                        if (!message.getSenderId().equals(AppManager.getInstance().userId)) {
                            unread_count += 1;
                        }
                    }

                    if (listener != null) {
                        listener.onUnreadMessages(chatId, unread_count);
                    }
                } else {
                    if (listener != null) {
                        listener.onUnreadMessages(chatId, 0);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChatListAdapter", databaseError.toString());
            }
        });

    }

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
        editor.putInt("raffles", user.getRaffles());
        editor.commit();
    }

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
        int raffles = sharedPreferences.getInt("raffles", 0);
        String chatsDic = sharedPreferences.getString("chats", null);
        Map<String,Object> chats = new Gson().fromJson(chatsDic, new TypeToken<Map<String, Object>>(){}.getType());
        String lastSeen = sharedPreferences.getString("lastseens", null);
        Map<String,Object> lastSeens = new Gson().fromJson(lastSeen, new TypeToken<Map<String, Object>>(){}.getType());
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
            data.put("lastseens", (chats != null) ? lastSeens : new HashMap<String, Object>());
            data.put("raffles", raffles);
            User user = new User(data);
            return user;
        } else {
            return null;
        }
    }

    public static void deleteSession(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uid", null);
        editor.commit();
    }

    public static void saveContact(Map<String,Contact> contacts){
        Context context = AppManager.getInstance().context;
        SharedPreferences sharedPreferences = context.getSharedPreferences("Contacts", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String contactsDic = gson.toJson(contacts);
        editor.putString("contacts", contactsDic);
        editor.commit();
    }

    public static Map<String, Contact> getContacts(){
        Context context = AppManager.getInstance().context;
        SharedPreferences sharedPreferences = context.getSharedPreferences("Contacts", Context.MODE_PRIVATE);
        String contactsDic = sharedPreferences.getString("contacts", null);
        Map<String,Contact> contacts = new Gson().fromJson(contactsDic, new TypeToken<Map<String, Contact>>(){}.getType());
        if (contacts == null)
            contacts = new HashMap<>();
        return contacts;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setUserValueListener(UserValueListener userValueListener) {
        this.userValueListener = userValueListener;
    }

    public void setUserValueListenerForChat(UserValueListener userValueListenerForChat) {
        this.userValueListenerForChat = userValueListenerForChat;
    }

    public void addNewContact(Uri contactUri, final ResultListener listener){
        String contactID = null;
        String contactNumber = null;
        String contactName = "";

        // getting contacts ID
        Cursor cursorID = context.getContentResolver().query(contactUri,
                new String[]{ContactsContract.Contacts._ID},
                null, null, null);
        if (cursorID.moveToFirst()) {
            contactID = cursorID.getString(cursorID.getColumnIndex(ContactsContract.Contacts._ID));
        }
        cursorID.close();
        Cursor cursorName = context.getContentResolver().query(contactUri, null, null, null, null);
        if (cursorName.moveToFirst()) {
            // DISPLAY_NAME = The display name for the contact.
            // HAS_PHONE_NUMBER =   An indicator of whether this contact has at least one phone number.
            contactName = cursorName.getString(cursorName.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }

        cursorName.close();

        // Using the contact ID now we will get contact phone number
        Cursor cursorPhone = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},

                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    /*ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                            ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,*/

                new String[]{contactID},
                null);

        if (cursorPhone.moveToFirst()) {
            contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        }

        cursorPhone.close();

        final String idx = contactID;
        final String name = contactName;
        final String phone = contactNumber.replace(" ", "").replace("-", "");

        Query query = usersRef.orderByChild("phone").equalTo(phone);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Map<String, Object> userData = (Map<String, Object>) child.getValue();
                        User user = new User(userData);
                        Contact contact = new Contact(idx, name, phone, user.getIdx(), user.getPhoto());
                        Map<String, Contact> contacts = AppManager.getContacts();
                        contacts.put(idx, contact);
                        AppManager.saveContact(contacts);
                        if (listener != null)
                            listener.onResult(true);
                    }
                } else {
                    if (listener != null)
                        listener.onResult(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.toString());
                if (listener != null)
                    listener.onResult(false);
            }
        });

        updatePhoneContacts();
    }

    public void updatePhoneContacts(){
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection    = new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor people = context.getContentResolver().query(uri, projection, null, null, null);

        int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        if(people.moveToFirst()) {
            do {
                String name   = people.getString(indexName);
                String number = people.getString(indexNumber);
                // Do work...
                if (number != null){
                    String key = number.replace(" ", "").replace("-", "");
                    phoneContacts.put(key, name);
                }

            } while (people.moveToNext());
        }

        Log.d("Contacts", phoneContacts.toString());
    }

    public boolean isExistingPhoneContact(String phonenumber){
        boolean isExisting = false;
        if (phoneContacts != null){
            for (Map.Entry<String, String> entry : phoneContacts.entrySet()){
                String contactPhone = entry.getKey();
                if (contactPhone.equals(phonenumber)) {
                    isExisting = true;
                    break;
                }
            }
        }

        return isExisting;
    }
}
