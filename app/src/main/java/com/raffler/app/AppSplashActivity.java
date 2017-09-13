package com.raffler.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ResultListener;
import com.raffler.app.models.User;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppSplashActivity extends AppCompatActivity {

    private static final String TAG = "AppSplashActivity";
    private static final long SPLASH_DURATION = 2500L;

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

        // Initialize image loader
        initImageLoader(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        handler.postDelayed(runnable, SPLASH_DURATION);
        if (AppManager.getSession() != null) {
            AppManager.getInstance().refreshPhoneContacts(new ResultListener() {
                @Override
                public void onResult(boolean success) {
                    Log.d(TAG, "didRefresh Contacts");
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(runnable);
    }

    private void dismissSplash(){
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null){
            AppManager.getInstance().startTrackingUser(firebaseUser.getUid());
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

    public static void initImageLoader(Context context) {

        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        //config.writeDebugLogs(); // Remove for release app
        ImageLoader.getInstance().init(config.build());
    }
}
