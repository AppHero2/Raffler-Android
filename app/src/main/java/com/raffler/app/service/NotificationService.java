package com.raffler.app.service;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationReceivedResult;
import com.raffler.app.R;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.models.Contact;

import org.json.JSONObject;

/**
 * Created by Ghost on 10/10/2017.
 */

public class NotificationService extends NotificationExtenderService {

    private static final String TAG = "NotificationService";

    @Override
    protected boolean onNotificationProcessing(OSNotificationReceivedResult receivedResult) {
        if (receivedResult != null) {
            JSONObject additionalData = receivedResult.payload.additionalData;
            if (additionalData != null) {
                final String senderPhone = additionalData.optString("sender_phone");
                final String senderPhoto = additionalData.optString("sender_photo");
                final String senderName = additionalData.optString("sender_name");
                try {
                    // phone must begin with '+'
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    Phonenumber.PhoneNumber numberProto = phoneUtil.parse(senderPhone, "");
                    final String countryCode = "+" + String.valueOf(numberProto.getCountryCode());
                    if (AppManager.getInstance().phoneContacts.isEmpty()) {
                        AppManager.getInstance().loadPhoneContacts(new ResultListener() {
                            @Override
                            public void onResult(boolean success) {
                                final Contact contact = AppManager.getInstance().getPhoneContact(senderPhone, countryCode);
                                if (senderName.equals("?"))
                                    showNotification(contact, senderPhone, senderPhoto);
                                else
                                    showNotification(contact, senderName, senderPhoto);
                            }
                        });
                    }else{
                        final Contact contact = AppManager.getInstance().getPhoneContact(senderPhone, countryCode);
                        if (senderName.equals("?"))
                            showNotification(contact, senderPhone, senderPhoto);
                        else
                            showNotification(contact, senderName, senderPhoto);
                    }

                    return true;
                } catch (NumberParseException e) {
                    System.err.println("NumberParseException was thrown: " + e.toString());
                }
            }
        }

        // Returning true tells the OneSignal SDK you have processed the notification and not to display it's own.
        return false;
    }

    private void showNotification(final Contact contact, final String senderName, final String senderPhoto) {

        if (contact != null) {
            ImageLoader.getInstance().loadImage(senderPhoto, new SimpleImageLoadingListener(){
                @Override
                public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage)
                {
                    OverrideSettings overrideSettings = new OverrideSettings();
                   if (loadedImage != null) {
                       overrideSettings.extender = new NotificationCompat.Extender() {
                           @Override
                           public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                               // Must disable the default sound when setting a custom one
                                /*builder.mNotification.flags &= ~Notification.DEFAULT_SOUND;
                                builder.setDefaults(builder.mNotification.flags);*/
                               return builder.setContentTitle(contact.getName())
//                                       .setColor(new BigInteger("FF333333", 16).intValue())
                                       .setLargeIcon(loadedImage);
                                /*.setSound(Uri.parse("content://media/internal/audio/media/32"))
                                .setColor(new BigInteger("FF00FF00", 16).intValue())
                                .setStyle(new NotificationCompat.BigTextStyle().bigText("[Modified Body(bigText)]"))
                                .setContentText("[Modified Body(ContentText)]");*/
                           }
                       };
                   } else {
                       final Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile_person);
                       overrideSettings.extender = new NotificationCompat.Extender() {
                           @Override
                           public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                               return builder.setContentTitle(contact.getName())
//                                       .setColor(new BigInteger("FF333333", 16).intValue())
                                       .setLargeIcon(bm);
                           }
                       };
                   }

                    displayNotification(overrideSettings);
                }
            });

        } else {
            OverrideSettings overrideSettings = new OverrideSettings();
            overrideSettings.extender = new NotificationCompat.Extender() {
                @Override
                public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                    return builder.setContentTitle(senderName);
                }
            };
            displayNotification(overrideSettings);
        }

    }
}
