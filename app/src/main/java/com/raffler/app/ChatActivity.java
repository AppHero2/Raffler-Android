package com.raffler.app;

import android.*;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.raffler.app.adapters.NewMessageListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.fragments.ChatFragment;
import com.raffler.app.interfaces.FileUploadListener;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.interfaces.UserValueListener;
import com.raffler.app.models.Chat;
import com.raffler.app.models.ChatType;
import com.raffler.app.models.Message;
import com.raffler.app.models.MessageStatus;
import com.raffler.app.models.MessageType;
import com.raffler.app.models.User;
import com.raffler.app.models.UserAction;
import com.raffler.app.utils.AmazonUtil;
import com.raffler.app.utils.ImagePicker;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

import static com.raffler.app.models.UserAction.TYPING;
import static com.raffler.app.models.UserStatus.ONLINE;

public class ChatActivity extends AppCompatActivity implements UserValueListener, View.OnClickListener{

    private static final int PICK_IMAGE_ID = 234; // the number doesn't matter
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

    private DatabaseReference usersRef, messagesRef, chatsRef, presenceRef, connectedUsersRef;
    private String chatId, lastMessageId;
    private ValueEventListener receiverValueEventListener;
    private ChildEventListener presenceValueEventListener;
    private NewMessageListener newMessageListener;

    private List<String> connectedUsers = new ArrayList<>();
    private Chat currentChat;
    private int raffles_point = 0;

    private LinearLayout mRevealView;
    private boolean hidden = true;
    private ImageButton btnGallery, btnPhoto, btnVideo, btnAudio, btnLocation, btnContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sender = AppManager.getSession();
        usersRef = References.getInstance().usersRef;
        Chat chat = AppManager.getInstance().selectedChat;
        chatId = Util.generateChatKeyFrom(sender.getIdx(), chat.getUserId());
        lastMessageId = chat.getMessageId() == null ? "" : chat.getMessageId();
        messagesRef = References.getInstance().messagesRef.child(chatId);
        chatsRef = References.getInstance().chatsRef.child(chatId);
        connectedUsersRef = chatsRef.child("connectedUser");

        raffles_point = sender.getRaffle_point();

        // detect user disconnected
        presenceRef = connectedUsersRef.child(sender.getIdx());
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

        mRevealView = (LinearLayout) findViewById(R.id.reveal_items);
        mRevealView.setVisibility(View.GONE);

        btnGallery = (ImageButton) findViewById(R.id.gallery_img_btn);
        btnPhoto = (ImageButton) findViewById(R.id.photo_img_btn);
        btnVideo = (ImageButton) findViewById(R.id.video_img_btn);
        btnAudio = (ImageButton) findViewById(R.id.audio_img_btn);
        btnLocation = (ImageButton) findViewById(R.id.location_img_btn);
        btnContact = (ImageButton) findViewById(R.id.contact_img_btn);

        btnGallery.setOnClickListener(this);
        btnPhoto.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnAudio.setOnClickListener(this);
        btnLocation.setOnClickListener(this);
        btnContact.setOnClickListener(this);

        ImageButton btnAttach = (ImageButton) findViewById(R.id.btn_attach);
        btnAttach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                showRevealView();
                onPickImage(view);
            }
        });

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
                DatabaseReference reference = usersRef.child(sender.getIdx()).child("userAction");
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

        chatFragment = ChatFragment.newInstance(sender.getIdx(), chatId, lastMessageId);
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
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode) {
            case PICK_IMAGE_ID:
                if(resultCode != RESULT_CANCELED){
                    /*if (data != null) {
                        if (data.getExtras() == null){
                            Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                            Log.e("FROM GALLERY OR PHOTOS", bitmap.toString());
                        }else{
                            Bitmap photo = (Bitmap) data.getExtras().get("data");
                            Log.e("FROM CAMERA", data.getExtras().toString());
                        }
                    }*/
                    try{
                        Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                        //create a file to write bitmap data
                        File f = new File(this.getCacheDir(), "tmp.jpg");
                        f.createNewFile();

                        //Convert bitmap to byte array
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        byte[] bitmapdata = bos.toByteArray();

                        //write the bytes in file
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(bitmapdata);
                        fos.flush();
                        fos.close();

                        Uri uri = Uri.fromFile(f);
                        sendPhoto(uri);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case REQUEST_GALLERY:
                if (resultCode != RESULT_CANCELED) {
                    if (data != null) {
                        mCurrentPhotoPath = data.getData();
                        if (mCurrentPhotoPath != null) {
                            sendPhoto(mCurrentPhotoPath);
                        }
                    }
                }
                break;
            case REQUEST_CAMERA:
                if (resultCode != RESULT_CANCELED) {
                    if (mCurrentPhotoPath != null) {
                        sendPhoto(mCurrentPhotoPath);
                    }
                }
                break;
            case REQUEST_CONTACT:
                if (resultCode == RESULT_OK) {
                    // Get the URI and query the content provider for the phone number
                    Uri contactUri = data.getData();

                    /* This method was removed at git version (v1.0.2 b51)
                    AppManager.getInstance().addNewContact(contactUri, new ResultListener() {
                        @Override
                        public void onResult(boolean success) {
                            if (!success){
                                Toast.makeText(ChatActivity.this, "Could not find that phone number.", Toast.LENGTH_SHORT).show();
                            }

                            updateContactName();
                        }
                    });*/
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTrackingReceiver();
        stopTrackingPresence();

        presenceRef.onDisconnect().cancel();
        presenceRef.removeValue();

        usersRef.child(sender.getIdx()).child("userAction").setValue(UserAction.IDLE.ordinal());
    }

    @Override
    protected void onPause() {
        super.onPause();

        presenceRef.onDisconnect().cancel();
        presenceRef.removeValue();

        usersRef.child(sender.getIdx()).child("userAction").setValue(UserAction.IDLE.ordinal());
    }

    @Override
    public void onClick(View v) {
        hideRevealView();
        switch (v.getId()) {
            case R.id.gallery_img_btn:
                galleryIntent();
                break;
            case R.id.photo_img_btn:
                try {
                    cameraIntent();
                }catch (IOException e){
                    e.printStackTrace();
                }
                break;
            case R.id.video_img_btn:

                break;
            case R.id.audio_img_btn:

                break;
            case R.id.location_img_btn:

                break;
            case R.id.contact_img_btn:

                break;
        }
    }

    @Override
    public void onLoadedUser(User user) {
        raffles_point = user.getRaffle_point();
        tvRafflePoints.setText(String.valueOf(raffles_point));
    }

    private void showRevealView() {
        int cx = (mRevealView.getLeft() + mRevealView.getRight());
        int cy = mRevealView.getTop();
        int radius = Math.max(mRevealView.getWidth(), mRevealView.getHeight());

        //Below Android LOLIPOP Version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SupportAnimator animator =
                    ViewAnimationUtils.createCircularReveal(mRevealView, cx, cy, 0, radius);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(700);

            SupportAnimator animator_reverse = animator.reverse();

            if (hidden) {
                mRevealView.setVisibility(View.VISIBLE);
                animator.start();
                hidden = false;
            } else {
                animator_reverse.addListener(new SupportAnimator.AnimatorListener() {
                    @Override
                    public void onAnimationStart() {

                    }

                    @Override
                    public void onAnimationEnd() {
                        mRevealView.setVisibility(View.INVISIBLE);
                        hidden = true;

                    }

                    @Override
                    public void onAnimationCancel() {

                    }

                    @Override
                    public void onAnimationRepeat() {

                    }
                });
                animator_reverse.start();
            }
        }
        // Android LOLIPOP And ABOVE Version
        else {
            if (hidden) {
                Animator anim = android.view.ViewAnimationUtils.createCircularReveal(mRevealView, cx, cy, 0, radius);
                mRevealView.setVisibility(View.VISIBLE);
                anim.start();
                hidden = false;
            } else {
                Animator anim = android.view.ViewAnimationUtils.createCircularReveal(mRevealView, cx, cy, radius, 0);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mRevealView.setVisibility(View.INVISIBLE);
                        hidden = true;
                    }
                });
                anim.start();
            }
        }
    }

    private void hideRevealView() {
        if (mRevealView.getVisibility() == View.VISIBLE) {
            mRevealView.setVisibility(View.GONE);
            hidden = true;
        }
    }

    private void loadData() {
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
        receiverValueEventListener = new ValueEventListener() {
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
                        long lastSeen = 0;
                        for (Map.Entry<String, Object> entry: receiver.getLastseens().entrySet()){
                            String key = entry.getKey();
                            if (key.equals(chatId)) {
                                lastSeen = (long)entry.getValue();
                                break;
                            }
                        }

                        if (lastSeen == 0) {
                            tvDescription.setText(R.string.chat_user_offline);
                        } else {
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        };

        usersRef.child(receiver.getIdx()).addValueEventListener(receiverValueEventListener);
    }

    private void stopTrackingReceiver(){
        if (receiverValueEventListener != null)
            usersRef.removeEventListener(receiverValueEventListener);
    }

    private void startTrackingPresence(){

        presenceValueEventListener = new ChildEventListener() {
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

        connectedUsersRef.addChildEventListener(presenceValueEventListener);
    }

    private void stopTrackingPresence(){
        if (presenceValueEventListener != null)
            connectedUsersRef.removeEventListener(presenceValueEventListener);
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

        final Message message = new Message(sender.getIdx(), messageData);
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
                headings.put("en", sender.getName().equals("?")?sender.getPhone():sender.getName());
                pushObject.put("headings", headings);
                pushObject.put("contents", contents);
                pushObject.put("include_player_ids", receivers);
                pushObject.put("android_group", chatId);
                JSONObject data = new JSONObject();
                data.put("sender_phone", sender.getPhone());
                data.put("sender_photo", sender.getPhoto());
                data.put("sender_name", sender.getName());
                pushObject.put("data", data);
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

    private void sendPhoto(Uri filePath){
        if (receiver == null)
            return;

        if (filePath == null)
            return;

        final DatabaseReference reference = messagesRef.push();
        final String messageId = reference.getKey();
        final long currentTime = System.currentTimeMillis();
        final Map<String, Object> messageData = new HashMap<>();
        final Map<String, Object> phones = new HashMap<>();
        phones.put(sender.getIdx(), sender.getPhone());
        phones.put(receiver.getIdx(), receiver.getPhone());
        final Map<String, Object> photos = new HashMap<>();
        photos.put(sender.getIdx(), sender.getPhoto()==null?"":sender.getPhoto());
        photos.put(receiver.getIdx(), receiver.getPhoto()==null?"":receiver.getPhoto());
        messageData.put("text", "");
        messageData.put("phones", phones);
        messageData.put("photos", photos);
        messageData.put("senderId", sender.getIdx());
        messageData.put("senderName", sender.getName());
        messageData.put("senderPhoto", sender.getPhoto());
        messageData.put("chatType", ChatType.PERSONAL.ordinal());
        messageData.put("messageType", MessageType.PHOTO.ordinal());
        messageData.put("status", MessageStatus.SENDING.ordinal());
        messageData.put("createdAt", currentTime);
        messageData.put("updatedAt", currentTime);
        messageData.put("idx", messageId);

        final Message message = new Message(sender.getIdx(), messageData);
        message.setAttachFilePath(filePath);
        if (newMessageListener != null) {
            newMessageListener.onGetNewMessage(message);
        }

        // upload photo to aws s3
        File fileToUpload = new File(filePath.getPath());
        uploadPhoto(fileToUpload, new FileUploadListener() {
            @Override
            public void onSuccess(boolean success, final String uploadedPath) {
                if (success) {
                    // update message delivery status
                    messageData.put("status", MessageStatus.SENT.ordinal());
                    reference.setValue(messageData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            messageData.put("resource", uploadedPath);
                            messageData.put("status", MessageStatus.DELIVERED.ordinal());
                            reference.setValue(messageData);
                            usersRef.child(sender.getIdx()).child("raffle_point").setValue(raffles_point + 1);
                        }
                    });

                    Map<String, Object> chatInfo = new HashMap<>();
                    chatInfo.put("phones", phones);
                    chatInfo.put("photos", photos);
                    chatInfo.put("lastSender", sender.getPhone());
                    chatInfo.put("lastMessage", getString(R.string.chat_send_photo));
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
                            contents.put("en", getString(R.string.chat_send_photo));
                            JSONObject headings = new JSONObject();
                            headings.put("en", sender.getName()==null?sender.getPhone():sender.getName());
                            pushObject.put("headings", headings);
                            pushObject.put("contents", contents);
                            pushObject.put("include_player_ids", receivers);
                            pushObject.put("android_group", chatId);
                            JSONObject data = new JSONObject();
                            data.put("sender_phone", sender.getPhone());
                            data.put("sender_photo", sender.getPhoto());
                            data.put("sender_name", sender.getName());
                            pushObject.put("data", data);
                            OneSignal.postNotification(pushObject, null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    chatsRef.updateChildren(messageData);

                    // analysis
                    Bundle params = new Bundle();
                    params.putString("sender", sender.getIdx());
                    References.getInstance().analytics.logEvent("send_photo", params);
                }
            }

            @Override
            public void onProgress(int percent) {
                Log.d(TAG, percent + "%");
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, e.toString());
            }
        });
    }

    public void onPickImage(View view) {
        Intent chooseImageIntent = ImagePicker.getPickImageIntent(this);
        startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
    }

    public static final int MULTIPLE_PERMISSIONS = 989;
    String[] permissions= new String[]{
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
    };

    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    private static final int REQUEST_CAMERA = 982;
    private static final int REQUEST_GALLERY = 983;
    private Uri mCurrentPhotoPath;
    private void cameraIntent() throws IOException
    {
        if (checkPermissions()){

            final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/picFolder/";
            File newdir = new File(dir);
            newdir.mkdirs();
            String file = dir+"photo.jpg";
            File newfile = new File(file);
            try {
                newfile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            mCurrentPhotoPath = Uri.fromFile(newfile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoPath);
            startActivityForResult(takePictureIntent, REQUEST_CAMERA);
        }
    }

    private void galleryIntent()
    {
        if (checkPermissions()){
            if (Build.VERSION.SDK_INT <= 19) {
                Intent intent = new Intent();
                intent.setType("image/jpeg");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_GALLERY);
            } else if (Build.VERSION.SDK_INT > 19) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_GALLERY);
            }
        }
    }

    private TransferUtility transferUtility;
    private TransferObserver transferObserver;
    private void uploadPhoto(File file, final FileUploadListener listener){
        long currentTime = System.currentTimeMillis();
        transferUtility = AmazonUtil.getTransferUtility(this);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/png");
        transferObserver = transferUtility.upload(
                "raffler-app/attached_Images",
                "IMG_" + currentTime + ".png",
                file,
                metadata
        );

        transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED){
                    final String fileName = transferObserver.getKey();
                    final String filePath = "https://s3.amazonaws.com/raffler-app/attached_Images/" + fileName;
                    Log.d(TAG, filePath);
                    if (listener != null) listener.onSuccess(true, filePath);
                } else {
                    //if (listener != null) listener.onResult(false);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                if (bytesTotal != 0) {
                    int percentage = (int) (bytesCurrent/bytesTotal * 100);
                    if (listener != null) listener.onProgress(percentage);
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                if (listener != null) listener.onError(ex);
            }
        });
    }
}
