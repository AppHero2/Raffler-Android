package com.raffler.app.service;

import android.app.Notification;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationReceivedResult;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.models.Contact;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Map;

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
                                showNotification(contact, senderName);
                            }
                        });
                    }else{
                        final Contact contact = AppManager.getInstance().getPhoneContact(senderPhone, countryCode);
                        showNotification(contact, senderName);
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

    private void showNotification(final Contact contact, final String senderName) {
        OverrideSettings overrideSettings = new OverrideSettings();

        if (contact != null) {
            overrideSettings.extender = new NotificationCompat.Extender() {
                @Override
                public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                    // Must disable the default sound when setting a custom one
                                /*builder.mNotification.flags &= ~Notification.DEFAULT_SOUND;
                                builder.setDefaults(builder.mNotification.flags);*/

                    return builder.setContentTitle(contact.getName());
                                /*.setSound(Uri.parse("content://media/internal/audio/media/32"))
                                .setColor(new BigInteger("FF00FF00", 16).intValue())
                                .setStyle(new NotificationCompat.BigTextStyle().bigText("[Modified Body(bigText)]"))
                                .setContentText("[Modified Body(ContentText)]");*/
                }
            };
        } else {
            overrideSettings.extender = new NotificationCompat.Extender() {
                @Override
                public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                    return builder.setContentTitle(senderName);
                }
            };
        }

        displayNotification(overrideSettings);
    }
}
