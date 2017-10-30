package com.raffler.app;

import android.app.Application;
import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.raffler.app.classes.AppManager;
import com.raffler.app.country.Country;
import com.raffler.app.models.RealmContactMigration;
import com.raffler.app.utils.References;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppApplication extends Application {

    private FirebaseDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                //.schemaVersion(2) // Must be bumped when the schema changes
                //.migration(new RealmContactMigration()) // Migration to run instead of throwing an exception
                .name("default.realm").build();
        Realm.setDefaultConfiguration(config);

        if(database==null) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        }

        database = FirebaseDatabase.getInstance();
        References.init(this, database);

        AppManager.getInstance().setContext(getApplicationContext());
        AppManager.getInstance().setCountry(Country.getCountryFromSIM(getApplicationContext()));

        // Initialize image loader
        initImageLoader(this);
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
