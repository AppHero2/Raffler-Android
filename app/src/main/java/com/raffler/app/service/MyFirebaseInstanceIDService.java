//package com.raffler.app.service;
//
///**
// * Created by Ghost on 10/17/2017.
// */
//
//import android.util.Log;
//
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.iid.FirebaseInstanceId;
//import com.google.firebase.iid.FirebaseInstanceIdService;
//import com.raffler.app.classes.AppManager;
//import com.raffler.app.models.User;
//
//import java.util.HashMap;
//import java.util.Map;
//
//
//public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
//
//    private static final String TAG = "MyFirebaseIIDService";
//
//    /**
//     * Called if InstanceID token is updated. This may occur if the security of
//     * the previous token had been compromised. Note that this is called when the InstanceID token
//     * is initially generated so this is where you would retrieve the token.
//     */
//    // [START refresh_token]
//    @Override
//    public void onTokenRefresh() {
//        // Get updated InstanceID token.
//        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//        Log.d(TAG, "Refreshed token: " + refreshedToken);
//
//        // If you want to send messages to this application instance or
//        // manage this apps subscriptions on the server side, send the
//        // Instance ID token to your app server.
//        sendRegistrationToServer(refreshedToken);
//    }
//    // [END refresh_token]
//
//    /**
//     * Persist token to third-party servers.
//     *
//     * Modify this method to associate the user's FCM InstanceID token with any server-side account
//     * maintained by your application.
//     *
//     * @param token The new token.
//     */
//    private void sendRegistrationToServer(String token) {
//        User user = AppManager.getSession();
//        if (user != null) {
//            FirebaseDatabase database = FirebaseDatabase.getInstance();
//            Map<String, Object> pushToken = new HashMap<>();
//            pushToken.put("pushToken", token);
//            database.getReference("Users").child(user.getIdx()).updateChildren(pushToken);
//            database.getReference("PushTokens").child(user.getIdx()).setValue(token);
//        }
//    }
//}