package com.raffler.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.raffler.app.alertView.AlertView;
import com.raffler.app.alertView.OnItemClickListener;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.models.User;
import com.raffler.app.tasks.LoadContactsTask;
import com.raffler.app.utils.References;
import com.raffler.app.utils.Util;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Ghost on 14/8/2017.
 */

public class AppSplashActivity extends AppCompatActivity {

    private static final String TAG = "AppSplashActivity";
    private static final long SPLASH_DURATION = 2000L;

    private Handler handler;
    private Runnable runnable;

    private String currentVersion;

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

        TextView tv_copyright = (TextView) findViewById(R.id.tv_copyright);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pInfo.versionName;
            String versionCode = String.valueOf(pInfo.versionCode);
            currentVersion = versionName + "(" + versionCode + ")";
            String copyright = getString(R.string.copyright_version) + currentVersion + getString(R.string.copyright_company);
            tv_copyright.setText(copyright);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

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

    /**
     * this method is used to check version number
     */
    private void checkVersionNumber(){

        final FirebaseRemoteConfig firebaseConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .build();
        firebaseConfig.setConfigSettings(configSettings);
        firebaseConfig.fetch(0)
        .addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // After config data is successfully fetched, it must be activated before newly fetched
                    // values are returned.
                    firebaseConfig.activateFetched();
                    long remoteVersion = firebaseConfig.getLong("androidVersion");
                    try {
                        PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        int versionCode = pInfo.versionCode;
                        if (remoteVersion > versionCode){
                            AlertView alertView = new AlertView(getString(R.string.alert_title_notice), "Your current version is "+ currentVersion +"\nYou must update this app to the latest version", getResources().getString(R.string.alert_button_okay), null, null, AppSplashActivity.this, AlertView.Style.Alert, new OnItemClickListener() {
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
                    Toast.makeText(AppSplashActivity.this, "Checking App Version Failed",
                            Toast.LENGTH_SHORT).show();

                    // checkVersionNumber
                    Util.wait(1000, new Runnable() {
                        @Override
                        public void run() {
                            checkVersionNumber();
                        }
                    });
                }
            }
        });

    }

    /**
     * this method is used to finish splash screen
     */
    private void dismissSplash(){
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null){
            AppManager.getInstance().startTrackingUser(firebaseUser.getUid());
            AppManager.getInstance().startTrackingNews(firebaseUser.getUid());
            User user = AppManager.getSession();
            if (user != null) {
                if (checkPermissions()) {
                    new LoadContactsTask(new ResultListener() {
                        @Override
                        public void onResult(boolean success) {
                            startActivity(new Intent(AppSplashActivity.this, MainActivity.class));
                        }
                    }).execute("");
                }
            } else {
                startActivity(new Intent(this, RegisterUserActivity.class));
            }
        } else {
            startActivity(new Intent(this, RegisterPhoneActivity.class));
        }
        this.finish();
    }

    
    public static final int MULTIPLE_PERMISSIONS = 989;

    String[] permissions= new String[]{
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
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
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permissions granted.
                    new LoadContactsTask(new ResultListener() {
                        @Override
                        public void onResult(boolean success) {
                            startActivity(new Intent(AppSplashActivity.this, MainActivity.class));
                        }
                    }).execute("");

                } else {
                    String permissionList = "";
                    for (String per : permissions) {
                        permissionList += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Toast.makeText(this, permissionList + "not granted.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
