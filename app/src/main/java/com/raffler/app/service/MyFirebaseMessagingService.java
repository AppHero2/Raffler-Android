//package com.raffler.app.service;
//
///**
// * Created by Ghost on 10/17/2017.
// */
//
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.media.RingtoneManager;
//import android.net.Uri;
//import android.os.Handler;
//import android.os.Looper;
//import android.support.v4.app.NotificationCompat;
//import android.util.Log;
//import android.view.View;
//
//import com.firebase.jobdispatcher.FirebaseJobDispatcher;
//import com.firebase.jobdispatcher.GooglePlayDriver;
//import com.firebase.jobdispatcher.Job;
//
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//import com.google.i18n.phonenumbers.NumberParseException;
//import com.google.i18n.phonenumbers.PhoneNumberUtil;
//import com.google.i18n.phonenumbers.Phonenumber;
//import com.nostra13.universalimageloader.core.ImageLoader;
//import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
//import com.onesignal.NotificationExtenderService;
//import com.raffler.app.MainActivity;
//import com.raffler.app.R;
//import com.raffler.app.classes.AppManager;
//import com.raffler.app.country.Country;
//import com.raffler.app.models.Contact;
//import com.raffler.app.utils.Util;
//
//import java.util.Map;
//
//public class MyFirebaseMessagingService extends FirebaseMessagingService {
//
//    private static final String TAG = "MyFirebaseMsgService";
//    Handler handler = new Handler(Looper.getMainLooper());
//    /**
//     * Called when message is received.
//     *
//     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
//     */
//    // [START receive_message]
//    @Override
//    public void onMessageReceived(RemoteMessage remoteMessage) {
//        // [START_EXCLUDE]
//        // There are two types of messages data messages and notification messages. Data messages are handled
//        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
//        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
//        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
//        // When the user taps on the notification they are returned to the app. Messages containing both notification
//        // and data payloads are treated as notification messages. The Firebase console always sends notification
//        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
//        // [END_EXCLUDE]
//
//        // TODO(developer): Handle FCM messages here.
//        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
//        Log.d(TAG, "From: " + remoteMessage.getFrom());
//
//        final RemoteMessage.Notification notification = remoteMessage.getNotification();
//        // Check if message contains a notification payload.
//        if (notification == null) return;
//
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile_person);
//
//        // Check if message contains a data payload.
//        Map<String, String> data = remoteMessage.getData();
//        if (data.size() > 0) {
//            if (data != null) {
//                final String senderPhone = data.get("sender_phone");
//                final String senderPhoto = data.get("sender_photo");
//                final String senderName = data.get("sender_name");
//
//                ImageLoader.getInstance().loadImage(senderPhoto, new SimpleImageLoadingListener(){
//                    @Override
//                    public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage)
//                    {
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile_person);
//                                if (loadedImage != null) {
//                                    bitmap = loadedImage;
//                                }
//
//                                Bitmap roundBitmap = Util.getCircleBitmap(bitmap);
//
//                                try {
//                                    // phone must begin with '+'
//                                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
//                                    Phonenumber.PhoneNumber numberProto = phoneUtil.parse(senderPhone, "");
//                                    final String countryCode = "+" + String.valueOf(numberProto.getCountryCode());
//                                    Map<String, String> contacts = AppManager.getContacts(getApplicationContext());
//                                    Country country = Country.getCountryFromSIM(getApplicationContext());
//                                    if (contacts.isEmpty()) {
//                                        sendNotification(notification.getTitle(), notification.getBody(), roundBitmap);
//                                    }else{
//                                        final Contact contact = getPhoneContact(senderPhone, countryCode, contacts, country);
//                                        if (contact != null) {
//                                            sendNotification(contact.getName(), notification.getBody(), roundBitmap);
//                                        } else {
//                                            sendNotification(notification.getTitle(), notification.getBody(), roundBitmap);
//                                        }
//
//                                    }
//
//                                } catch (NumberParseException e) {
//                                    System.err.println("NumberParseException was thrown: " + e.toString());
//                                }
//
//                            }
//                        });
//
//                    }
//                });
//            }
//        } else {
//            sendNotification(notification.getTitle(), notification.getBody(), bitmap);
//        }
//
//        // Also if you intend on generating your own notifications as a result of a received FCM
//        // message, here is where that should be initiated. See sendNotification method below.
//    }
//    // [END receive_message]
//
//    /**
//     * Schedule a job using FirebaseJobDispatcher.
//     */
//    private void scheduleJob() {
//        // [START dispatch_job]
//        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
//        Job myJob = dispatcher.newJobBuilder()
//                .setService(MyJobService.class)
//                .setTag("my-job-tag")
//                .build();
//        dispatcher.schedule(myJob);
//        // [END dispatch_job]
//    }
//
//    /**
//     * Handle time allotted to BroadcastReceivers.
//     */
//    private void handleNow() {
//        Log.d(TAG, "Short lived task is done.");
//    }
//
//    /**
//     * Create and show a simple notification containing the received FCM message.
//     *
//     * @param message FCM message body received.
//     */
//    private void sendNotification(String title, String message, Bitmap bitmap) {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                PendingIntent.FLAG_ONE_SHOT);
//
//        String channelId = getString(R.string.default_notification_channel_id);
//        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(this, channelId)
//                        .setSmallIcon(R.drawable.ic_stat_onesignal_default)
//                        .setLargeIcon(bitmap)
//                        .setContentTitle(title)
//                        .setContentText(message)
//                        .setAutoCancel(true)
//                        .setSound(defaultSoundUri)
//                        .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
//    }
//
//    public Contact getPhoneContact(String phone, String countryCode, Map<String, String> contacts, Country country) {
//        Contact existing_contact = null;
//        for (Map.Entry<String, String> entry : contacts.entrySet()) {
//            String contactPhone = entry.getKey();
//            String contactName = entry.getValue();
//            if (!contactPhone.contains("+")){
//
//                String regionCode = country.getDialCode();
//                contactPhone = regionCode + contactPhone;
//                if (phone.contains(regionCode)){
//                    String nationalPhoneNumber = phone.replace(regionCode, "");
//                    if (contactPhone.contains(nationalPhoneNumber)) {
//                        existing_contact = new Contact(null, contactName, contactPhone);
//                        break;
//                    }
//                }
//            } else {
//                if (contactPhone.contains(countryCode)){
//                    String nationalPhoneNumber = phone.replace(countryCode, "");
//                    if (contactPhone.contains(nationalPhoneNumber)) {
//                        existing_contact = new Contact(null, contactName, contactPhone);
//                        break;
//                    }
//                }
//            }
//        }
//        return existing_contact;
//    }
//}
