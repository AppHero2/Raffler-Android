package com.raffler.app;

import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.raffler.app.classes.AppManager;
import com.raffler.app.fragments.ChatFragment;
import com.raffler.app.models.Message;
import com.raffler.app.models.MessageStatus;
import com.raffler.app.models.MessageType;
import com.raffler.app.models.User;
import com.raffler.app.models.UserStatus;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.raffler.app.models.UserAction.TYPING;
import static com.raffler.app.models.UserStatus.ONLINE;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = ChatActivity.class.getSimpleName();
    private EditText messageEditText;
    private TextView titleToolbarTextView;
    private TextView descToolbarTextView;
    private User sender, receiver;
    private FrameLayout btnSend;
    private ChatFragment chatFragment;

    private DatabaseReference usersRef, messagesRef, chatsRef;
    private String chatId;
    private ValueEventListener receiverValueEventListenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        usersRef = References.getInstance().usersRef;
        chatId = Util.generateChatKeyFrom(AppManager.getInstance().userId, AppManager.getInstance().selectedUser.getIdx());
        messagesRef = References.getInstance().messagesRef.child(chatId);
        chatsRef = References.getInstance().chatsRef.child(chatId);

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
                /*DatabaseReference reference = ref.getUser()
                        .status(Common.getSimSerialNumber(ChatActivity.this))
                        .child(Constant.ACTION);
                if (charSequence.length() > 0) {
                    reference.setValue(1);
                } else {
                    reference.setValue(0);
                }*/
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

        trackReceiverStatus();

        chatFragment = ChatFragment.newInstance(chatId);
        try {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, chatFragment).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTrackingReceiver();
    }

    private void loadData() {
        sender = AppManager.getSession(this);
        receiver = AppManager.getInstance().selectedUser;

        if (receiver == null)
            finish();
        else {
            titleToolbarTextView.setText(receiver.getName());
        }
    }

    private void trackReceiverStatus(){
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
                        descToolbarTextView.setText(R.string.chat_user_lastseen);
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

    private void sendMessage(){
        final String text = messageEditText.getText().toString();
        if (text.trim().length() == 0) {
            return;
        }

        long miliSeconds = System.currentTimeMillis();
        long currentTime = miliSeconds/1000;
        final Map<String, Object> messageData = new HashMap<>();
        messageData.put("idx", miliSeconds);
        messageData.put("text", text);
        messageData.put("senderId", sender.getIdx());
        messageData.put("senderName", sender.getName());
        messageData.put("messageType", MessageType.TEXT.ordinal());
        messageData.put("status", MessageStatus.SENDING.ordinal());
        messageData.put("createdAt", currentTime);
        messageData.put("updatedAt", currentTime);

        final Message message = new Message(messageData);

        final DatabaseReference reference = messagesRef.push();
        reference.setValue(messageData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                messageData.put("status", MessageStatus.SENT.ordinal());
                reference.setValue(messageData);
                message.updateValue(messageData);
            }
        });

        if (receiver.getUserStatus() != ONLINE) {
            try {
                JSONObject pushObject = new JSONObject("{'contents': {'en':'"+ message +"'}, 'include_player_ids': ['" + receiver.getPushToken() + "']}");
                OneSignal.postNotification(pushObject, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        chatsRef.setValue(messageData);

        Map<String, Object> chat = new HashMap<>();
        chat.put(chatId, currentTime);
        usersRef.child(sender.getIdx()).child("chats").updateChildren(chat);
        usersRef.child(receiver.getIdx()).child("chats").updateChildren(chat);
    }
}
