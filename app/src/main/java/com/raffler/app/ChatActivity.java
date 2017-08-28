package com.raffler.app;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.raffler.app.adapters.NewMessageListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.fragments.ChatFragment;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.ChatType;
import com.raffler.app.models.Message;
import com.raffler.app.models.MessageStatus;
import com.raffler.app.models.MessageType;
import com.raffler.app.models.User;
import com.raffler.app.models.UserAction;
import com.raffler.app.models.UserStatus;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.raffler.app.models.UserAction.TYPING;
import static com.raffler.app.models.UserStatus.ONLINE;

public class ChatActivity extends AppCompatActivity implements UserValueListener{

    private static final String TAG = ChatActivity.class.getSimpleName();

    private EditText messageEditText;
    private TextView titleToolbarTextView;
    private TextView descToolbarTextView;
    private TextView txtRafflesCount;
    private User sender, receiver;
    private FrameLayout btnSend;
    private ChatFragment chatFragment;

    private DatabaseReference usersRef, messagesRef, chatsRef, presenceRef, connnectedUsersRef;
    private String chatId, lastMessageId;
    private ValueEventListener receiverValueEventListenter;
    private ChildEventListener presenceValueEventListenter;
    private NewMessageListener newMessageListener;

    private List<String> connectedUsers = new ArrayList<>();

    private int raffles_count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        usersRef = References.getInstance().usersRef;
        Chat chat = AppManager.getInstance().selectedChat;
        chatId = Util.generateChatKeyFrom(AppManager.getInstance().userId, chat.getUser().getIdx());
        lastMessageId = chat.getMessage() == null ? "" : chat.getMessage().getIdx();
        messagesRef = References.getInstance().messagesRef.child(chatId);
        chatsRef = References.getInstance().chatsRef.child(chatId);
        connnectedUsersRef = chatsRef.child("connectedUser");

        String userId = AppManager.getInstance().userId;
        raffles_count = AppManager.getSession().getRaffles();

        // detect user disconnected
        presenceRef = connnectedUsersRef.child(userId);
        presenceRef.onDisconnect().removeValue();

        // register user as connected
        presenceRef.setValue(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        titleToolbarTextView = (TextView) toolbar.findViewById(R.id.textView_toolbar_title);
        descToolbarTextView = (TextView) toolbar.findViewById(R.id.textView_toolbar_description);
        descToolbarTextView.setText(null);
        txtRafflesCount = (TextView) toolbar.findViewById(R.id.tv_count);
        txtRafflesCount.setText(String.valueOf(raffles_count));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        messageEditText = (EditText) findViewById(R.id.editText_message);
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                DatabaseReference reference = usersRef.child(AppManager.getInstance().userId).child("userAction");
                if (charSequence.length() > 0) {
                    reference.setValue(UserAction.TYPING.ordinal());
                } else {
                    reference.setValue(UserAction.IDLE.ordinal());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnSend = (FrameLayout) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        loadData();

        startTrackingReceiver();
        startTrackingPresence();

        chatFragment = ChatFragment.newInstance(chatId, lastMessageId);
        this.newMessageListener = chatFragment.messageListener;
        try {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, chatFragment).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AppManager.getInstance().setUserValueListenerForChat(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTrackingReceiver();
        stopTrackingPresence();

        presenceRef.onDisconnect().cancel();
        presenceRef.removeValue();
    }

    @Override
    protected void onPause() {
        super.onPause();

        presenceRef.onDisconnect().cancel();
        presenceRef.removeValue();
    }

    @Override
    public void onLoadedUser(User user) {
        raffles_count = user.getRaffles();
        txtRafflesCount.setText(String.valueOf(raffles_count));
    }

    private void loadData() {
        sender = AppManager.getSession();
        receiver = AppManager.getInstance().selectedChat.getUser();

        if (receiver == null)
            finish();
        else {
            titleToolbarTextView.setText(receiver.getName());
        }
    }

    private void startTrackingReceiver(){
        receiverValueEventListenter = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> userData = (Map<String, Object>) dataSnapshot.getValue();
                    receiver = new User(userData);
                    if (receiver.getUserAction() == TYPING) {
                        descToolbarTextView.setText(R.string.chat_user_typing);
                    } else if (receiver.getUserStatus() == ONLINE) {
                        descToolbarTextView.setText(R.string.chat_user_online);
                    } else {
                        long lastSeen = System.currentTimeMillis();
                        for (Map.Entry<String, Object> entry: receiver.getLastseens().entrySet()){
                            String key = entry.getKey();
                            if (key.equals(chatId)) {
                                lastSeen = (long)entry.getValue();
                                break;
                            }
                        }

                        descToolbarTextView.setText(
                                String.format(
                                        Locale.getDefault(),
                                        "%s %s at %s",
                                        getResources().getString(R.string.chat_user_lastseen),
                                        Util.getUserFriendlyDateForChat(
                                                ChatActivity.this, lastSeen
                                        ).toLowerCase(),
                                        Util.getUserTime(lastSeen)
                                )
                        );
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        usersRef.child(receiver.getIdx()).addValueEventListener(receiverValueEventListenter);
    }

    private void stopTrackingReceiver(){
        usersRef.removeEventListener(receiverValueEventListenter);
    }

    private void startTrackingPresence(){

        presenceValueEventListenter = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String userId = dataSnapshot.getKey();
                if (!connectedUsers.contains(userId)){
                    connectedUsers.add(userId);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String userId = dataSnapshot.getKey();
                if (connectedUsers.contains(userId)){
                    connectedUsers.remove(userId);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        connnectedUsersRef.addChildEventListener(presenceValueEventListenter);
    }

    private void stopTrackingPresence(){
        connnectedUsersRef.removeEventListener(presenceValueEventListenter);
    }

    private void sendMessage(){
        final String text = messageEditText.getText().toString();
        if (text.trim().length() == 0) {
            return;
        }

        final DatabaseReference reference = messagesRef.push();

        long currentTime = System.currentTimeMillis();
        final Map<String, Object> messageData = new HashMap<>();

        messageData.put("text", text);
        messageData.put("senderId", sender.getIdx());
        messageData.put("senderName", sender.getName());
        messageData.put("senderPhoto", sender.getPhoto());
        messageData.put("chatType", ChatType.PERSONAL.ordinal());
        messageData.put("messageType", MessageType.TEXT.ordinal());
        messageData.put("status", MessageStatus.SENDING.ordinal());
        messageData.put("createdAt", currentTime);
        messageData.put("updatedAt", currentTime);
        messageData.put("idx", reference.getKey());

        final Message message = new Message(messageData);
        if (newMessageListener != null) {
            newMessageListener.onGetNewMessage(message);
        }

        messageData.put("status", MessageStatus.SENT.ordinal());
        reference.setValue(messageData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                messageData.put("status", MessageStatus.DELIVERED.ordinal());
                reference.setValue(messageData);
                usersRef.child(sender.getIdx()).child("raffles").setValue(raffles_count+1);
            }
        });

        // send push notification
        if (!connectedUsers.contains(receiver.getIdx())) {
            try {
                JSONArray receivers = new JSONArray();
                receivers.put(receiver.getPushToken());
                JSONObject pushObject = new JSONObject();
                JSONObject contents = new JSONObject();
                contents.put("en", message.getText());
                JSONObject headings = new JSONObject();
                headings.put("en", sender.getName());
                pushObject.put("headings", headings);
                pushObject.put("contents", contents);
                pushObject.put("include_player_ids", receivers);
                OneSignal.postNotification(pushObject, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        chatsRef.updateChildren(messageData);

        Map<String, Object> chat = new HashMap<>();
        chat.put(chatId, currentTime);
        usersRef.child(sender.getIdx()).child("chats").updateChildren(chat);
        usersRef.child(receiver.getIdx()).child("chats").updateChildren(chat);

        messageEditText.setText(null);

    }
}
