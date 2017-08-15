package com.raffler.app.classes;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppManager {
    private static final AppManager ourInstance = new AppManager();

    public static AppManager getInstance() {
        return ourInstance;
    }

    private AppManager() {
    }
}
