package com.raffler.app.utils;

import android.content.Context;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by Ghost on 22/8/2017.
 */

public class References {
    private static References instance;
    private final User user;
    private final Chat chat;
    private FirebaseDatabase database;
    private Context context;

    public DatabaseReference usersRef, contactsRef, chatsRef, messagesRef;

    public static void init(Context context, FirebaseDatabase database) {
        instance = new References(context, database);
    }

    public static References getInstance() {
        return instance;
    }

    private References(Context context, FirebaseDatabase database) {
        this.context = context;
        this.database = database;

        user = new User();
        chat = new Chat();

        usersRef = database.getReference(Constant.USER);
        contactsRef = database.getReference(Constant.CONTACTS);
        chatsRef = database.getReference(Constant.CHAT);
        messagesRef = database.getReference(Constant.MESSAGES);
    }


    public User getUser() {
        return user;
    }

    public Chat getChat() {
        return chat;
    }

    public class User {
        DatabaseReference reference = database.getReference(Constant.USER);
        public DatabaseReference getReference() {
            return reference;
        }

        public DatabaseReference getReference(String key) {
            return reference.child(key);
        }

        public DatabaseReference information(String serial) {
            return getReference(serial)
                    .child(Constant.INFORMATION);
        }

        public DatabaseReference message(String serial) {
            return getReference(serial)
                    .child(Constant.MESSAGES);
        }

        public DatabaseReference status(String serial) {
            return getReference(serial)
                    .child(Constant.STATUS);
        }


        public DatabaseReference notification(String serial) {
            return getReference(serial).child(Constant.NOTIFICATION);
        }
    }

    public class Chat {
        DatabaseReference reference = database.getReference(Constant.CHAT);

        public DatabaseReference getReference() {
            return reference;
        }
        public DatabaseReference getReference(String key) {
            return reference.child(key);
        }

        public DatabaseReference message(String key) {
            return getReference(key).child(Constant.MESSAGES);
        }

        public DatabaseReference meta(String key) {
            return getReference(key).child(Constant.META);
        }
    }

}
