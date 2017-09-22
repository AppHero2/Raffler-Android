package com.raffler.app;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.onesignal.OneSignal;
import com.raffler.app.adapters.NewMessageListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.fragments.ChatFragment;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.ChatType;
import com.raffler.app.models.Contact;
import com.raffler.app.models.Message;
import com.raffler.app.models.MessageStatus;
import com.raffler.app.models.MessageType;
import com.raffler.app.models.User;
import com.raffler.app.models.UserAction;
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

    private static final int REQUEST_CONTACT = 567;
    private static final String TAG = ChatActivity.class.getSimpleName();

    private EditText etMessageSend;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvRafflePoints;
    private User sender, receiver;
    private FrameLayout btnSend;
    private ChatFragment chatFragment;
    private LinearLayout layoutTopBanner;
    private RelativeLayout layoutRefresh;
    private ProgressBar progressBar;
    private AppCompatButton btnBlock, btnAdd;

    private DatabaseReference usersRef, messagesRef, chatsRef, presenceRef, connnectedUsersRef;
    private String chatId, lastMessageId;
    private ValueEventListener receiverValueEventListenter;
    private ChildEventListener presenceValueEventListenter;
    private NewMessageListener newMessageListener;

    private List<String> connectedUsers = new ArrayList<>();
    private Chat currentChat;
    private int raffles_point = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        usersRef = References.getInstance().usersRef;
        Chat chat = AppManager.getInstance().selectedChat;
        chatId = Util.generateChatKeyFrom(AppManager.getInstance().userId, chat.getUserId());
        lastMessageId = chat.getMessageId() == null ? "" : chat.getMessageId();
        messagesRef = References.getInstance().messagesRef.child(chatId);
        chatsRef = References.getInstance().chatsRef.child(chatId);
        connnectedUsersRef = chatsRef.child("connectedUser");

        String userId = AppManager.getInstance().userId;
        raffles_point = AppManager.getSession().getRaffle_point();

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

        layoutTopBanner = (LinearLayout) findViewById(R.id.layout_top_banner);
        layoutTopBanner.setVisibility(View.GONE);
        btnBlock = (AppCompatButton) findViewById(R.id.btnBlock);
        btnBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        btnAdd = (AppCompatButton) findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION, ContactsContract.Contacts.CONTENT_URI);
                // Sets the MIME type to match the Contacts Provider
                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                // Inserts a Phone number
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, receiver.getPhone());
                startActivityForResult(intent, REQUEST_CONTACT);*/
            }
        });

        layoutRefresh = (RelativeLayout) findViewById(R.id.layout_refresh);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        tvTitle = (TextView) toolbar.findViewById(R.id.textView_toolbar_title);
        tvDescription = (TextView) toolbar.findViewById(R.id.textView_toolbar_description);
        tvDescription.setText(null);
        tvRafflePoints = (TextView) toolbar.findViewById(R.id.tv_count);
        tvRafflePoints.setText(String.valueOf(raffles_point));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        etMessageSend = (EditText) findViewById(R.id.editText_message);
        etMessageSend.addTextChangedListener(new TextWatcher() {
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

        chatFragment = ChatFragment.newInstance(chatId, lastMessageId);
        chatFragment.setResultListener(new ResultListener() {
            @Override
            public void onResult(boolean success) {
                layoutRefresh.setVisibility(View.GONE);
            }
        });
        this.newMessageListener = chatFragment.messageListener;
        try {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, chatFragment).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AppManager.getInstance().setUserValueListenerForChat(this);

        currentChat = AppManager.getInstance().selectedChat;

        loadData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK) {
            // Get the URI and query the content provider for the phone number
            Uri contactUri = data.getData();

            AppManager.getInstance().addNewContact(contactUri, new ResultListener() {
                @Override
                public void onResult(boolean success) {
                    if (!success){
                        Toast.makeText(ChatActivity.this, "Could not find that phone number.", Toast.LENGTH_SHORT).show();
                    }

                    updateContactName();
                }
            });
        }
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
        raffles_point = user.getRaffle_point();
        tvRafflePoints.setText(String.valueOf(raffles_point));
    }

    private void updateContactName() {

        String receiverPhone = receiver.getPhone();
        String phoneContactId = AppManager.getPhoneContactId(receiverPhone);
        if (phoneContactId == null) {
            tvTitle.setText(receiverPhone);
            try {
                // phone must begin with '+'
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber numberProto = phoneUtil.parse(receiverPhone, "");
                                    /*int countryCode = numberProto.getCountryCode();
                                    long number = numberProto.getNationalNumber();*/
                String formatedPhoneNumber = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                tvTitle.setText(formatedPhoneNumber);

            } catch (NumberParseException e) {
                System.err.println("NumberParseException was thrown: " + e.toString());
            }
        } else {
            Contact contact = AppManager.getContacts().get(phoneContactId);
            String contactName = contact.getName();
            if (contactName == null)
                tvTitle.setText(receiverPhone);
            else
                tvTitle.setText(contactName);
        }
    }

    private void loadData() {

        sender = AppManager.getSession();
        String title = currentChat.getTitle();
        tvTitle.setText(title);
        String contactId = currentChat.getUserId();

        AppManager.getUser(contactId, new UserValueListener() {
            @Override
            public void onLoadedUser(User user) {
                receiver = user;
                startTrackingReceiver();
                startTrackingPresence();
            }
        });

        usersRef.child(sender.getIdx()).child("chats").child(chatId).child("unreadCount").setValue(0);
    }

    private void startTrackingReceiver(){
        receiverValueEventListenter = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    Map<String, Object> userData = (Map<String, Object>) dataSnapshot.getValue();
                    receiver = new User(userData);
                    if (receiver.getUserAction() == TYPING) {
                        tvDescription.setText(R.string.chat_user_typing);
                    } else if (receiver.getUserStatus() == ONLINE) {
                        tvDescription.setText(R.string.chat_user_online);
                    } else {
                        long lastSeen = System.currentTimeMillis();
                        for (Map.Entry<String, Object> entry: receiver.getLastseens().entrySet()){
                            String key = entry.getKey();
                            if (key.equals(chatId)) {
                                lastSeen = (long)entry.getValue();
                                break;
                            }
                        }

                        tvDescription.setText(
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

        if (receiver == null)
            return;

        final String text = etMessageSend.getText().toString();
        if (text.trim().length() == 0) {
            return;
        }

        final DatabaseReference reference = messagesRef.push();
        String messageId = reference.getKey();
        long currentTime = System.currentTimeMillis();
        final Map<String, Object> messageData = new HashMap<>();
        Map<String, Object> phones = new HashMap<>();
        phones.put(sender.getIdx(), sender.getPhone());
        phones.put(receiver.getIdx(), receiver.getPhone());
        Map<String, Object> photos = new HashMap<>();
        photos.put(sender.getIdx(), sender.getPhoto()==null?"":sender.getPhoto());
        photos.put(receiver.getIdx(), receiver.getPhoto()==null?"":receiver.getPhoto());
        messageData.put("text", text);
        messageData.put("phones", phones);
        messageData.put("photos", photos);
        messageData.put("senderId", sender.getIdx());
        messageData.put("senderName", sender.getName());
        messageData.put("senderPhoto", sender.getPhoto());
        messageData.put("chatType", ChatType.PERSONAL.ordinal());
        messageData.put("messageType", MessageType.TEXT.ordinal());
        messageData.put("status", MessageStatus.SENDING.ordinal());
        messageData.put("createdAt", currentTime);
        messageData.put("updatedAt", currentTime);
        messageData.put("idx", messageId);

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
                usersRef.child(sender.getIdx()).child("raffle_point").setValue(raffles_point +1);
            }
        });

        Map<String, Object> chatInfo = new HashMap<>();
        chatInfo.put("phones", phones);
        chatInfo.put("photos", photos);
        chatInfo.put("lastSender", sender.getPhone());
        chatInfo.put("lastMessage", text);
        chatInfo.put("lastMessageId", messageId);
        chatInfo.put("updatedAt", currentTime);
        usersRef.child(sender.getIdx()).child("chats").child(chatId).updateChildren(chatInfo);
        usersRef.child(receiver.getIdx()).child("chats").child(chatId).updateChildren(chatInfo);

        if (!connectedUsers.contains(receiver.getIdx())) {

            // update unread message count
            Query query = usersRef.child(receiver.getIdx()).child("chats").child(chatId).child("unreadCount");
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        long unreadCount = (long)dataSnapshot.getValue();
                        usersRef.child(receiver.getIdx()).child("chats").child(chatId).child("unreadCount").setValue(unreadCount+1);
                    } else {
                        usersRef.child(receiver.getIdx()).child("chats").child(chatId).child("unreadCount").setValue(1);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    FirebaseCrash.report(databaseError.toException());
                }
            });

            // send push notification
            try {
                JSONArray receivers = new JSONArray();
                receivers.put(receiver.getPushToken());
                JSONObject pushObject = new JSONObject();
                JSONObject contents = new JSONObject();
                contents.put("en", message.getText());
                JSONObject headings = new JSONObject();
                headings.put("en", sender.getName()==null?sender.getPhone():sender.getName());
                pushObject.put("headings", headings);
                pushObject.put("contents", contents);
                pushObject.put("include_player_ids", receivers);
                OneSignal.postNotification(pushObject, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        chatsRef.updateChildren(messageData);

        etMessageSend.setText(null);

        // analysis
        Bundle params = new Bundle();
        params.putString("sender", sender.getIdx());
        References.getInstance().analytics.logEvent("send_message", params);
    }
}
