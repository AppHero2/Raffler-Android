package com.raffler.app;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.raffler.app.utils.References;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppApplication extends Application {

    private FirebaseDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();

        if(database==null) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);

        }
        database = FirebaseDatabase.getInstance();
        References.init(this, database);
    }
}
