package com.raffler.app;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
