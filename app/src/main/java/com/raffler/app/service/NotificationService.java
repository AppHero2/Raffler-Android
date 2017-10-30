package com.raffler.app.service;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
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
import com.raffler.app.country.Country;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.models.Contact;
import com.raffler.app.models.RealmContact;
import com.raffler.app.utils.Util;

import org.json.JSONObject;

import java.util.Map;

import io.realm.RealmList;

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
                    RealmList<RealmContact> contacts = AppManager.getContacts(getApplicationContext());
                    Country country = Country.getCountryFromSIM(getApplicationContext());
                    final Contact contact = getPhoneContact(senderPhone, countryCode, contacts, country);
                    if (senderName.equals("?"))
                        showNotification(contact, senderPhone, senderPhoto);
                    else
                        showNotification(contact, senderName, senderPhoto);

                } catch (NumberParseException e) {
                    System.err.println("NumberParseException was thrown: " + e.toString());
                }
                return true;
            } else {
                return false;
            }

        } else {
            // Returning true tells the OneSignal SDK you have processed the notification and not to display it's own.
            return false;
        }
    }

    public Contact getPhoneContact(String phone, String countryCode, RealmList<RealmContact> contacts, Country country) {
        Contact existing_contact = null;
        for (RealmContact contact: contacts){
            String contactPhone = contact.getPhone();
            String contactName = contact.getName();
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


    Handler handler = new Handler(Looper.getMainLooper());

    private void showNotification(final Contact contact, final String senderName, final String senderPhoto) {

        if (contact != null) {
            ImageLoader.getInstance().loadImage(senderPhoto, new SimpleImageLoadingListener(){
                @Override
                public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            OverrideSettings overrideSettings = new OverrideSettings();
                            if (loadedImage != null) {
                                overrideSettings.extender = new NotificationCompat.Extender() {
                                    @Override
                                    public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                                        // Must disable the default sound when setting a custom one
                                                /*builder.mNotification.flags &= ~Notification.DEFAULT_SOUND;
                                                builder.setDefaults(builder.mNotification.flags);*/
                                        Bitmap roundBitmap = Util.getCircleBitmap(loadedImage);
                                        return builder.setContentTitle(contact.getName())
                                                //.setColor(new BigInteger("FF333333", 16).intValue())
                                                .setLargeIcon(roundBitmap);
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
                                                .setLargeIcon(bm);
                                    }
                                };
                            }

                            displayNotification(overrideSettings);
                        }
                    });

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
