package com.raffler.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppSplashActivity extends AppCompatActivity {

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        handler.postDelayed(runnable, SPLASH_DURATION);
    }

    @Override
    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(runnable);
    }

    private void dismissSplash(){
        startActivity(new Intent(this, RegisterPhoneActivity.class));
        this.finish();
    }
}
