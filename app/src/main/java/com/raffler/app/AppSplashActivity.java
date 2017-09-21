package com.raffler.app;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.raffler.app.alertView.AlertView;
import com.raffler.app.alertView.OnItemClickListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.models.User;
import com.raffler.app.utils.References;


/**
 * Created by Ghost on 14/8/2017.
 */

public class AppSplashActivity extends AppCompatActivity {

    private static final String TAG = "AppSplashActivity";
    private static final long SPLASH_DURATION = 2000L;

    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                dismissSplash();
            }
        };

        // allow user to click and dismiss the splash screen prematurely
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissSplash();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkVersionNumber();
    }

    @Override
    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(runnable);
    }

    private void checkVersionNumber(){
        Query query = References.getInstance().versionRef.child("android");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null){
                    long serverVersion = (long)dataSnapshot.getValue();
                    try {
                        PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        int versionCode = pInfo.versionCode;
                        if (serverVersion > versionCode){
                            AlertView alertView = new AlertView(getString(R.string.alert_title_notice), "You must update this app to the latest version", getResources().getString(R.string.alert_button_okay), null, null, AppSplashActivity.this, AlertView.Style.Alert, new OnItemClickListener() {
                                @Override
                                public void onItemClick(Object o, int position) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.raffler.app"));
                                    startActivity(browserIntent);
                                }
                            });
                            alertView.show();
                        } else {
                            handler.postDelayed(runnable, SPLASH_DURATION);
                        }

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    handler.postDelayed(runnable, SPLASH_DURATION);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handler.postDelayed(runnable, SPLASH_DURATION);
            }
        });
    }

    private void dismissSplash(){
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null){
            AppManager.getInstance().startTrackingUser(firebaseUser.getUid());
            AppManager.getInstance().startTrackingNews(firebaseUser.getUid());
            User user = AppManager.getSession();
            if (user != null) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, RegisterUserActivity.class));
            }
        } else {
            startActivity(new Intent(this, RegisterPhoneActivity.class));
        }
        this.finish();
    }
}
