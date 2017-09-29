package com.raffler.app.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.raffler.app.classes.AppManager;
import com.raffler.app.interfaces.ResultListener;

/**
 * Created by Ghost on 9/26/2017.
 */

public class LoadContactsTask extends AsyncTask<String, Void, String> {

    private static final String TAG = "LoadContactsTask";
    private ResultListener listener;

    public LoadContactsTask(ResultListener listener){
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        AppManager.getInstance().loadPhoneContacts(new ResultListener() {
            @Override
            public void onResult(boolean success) {
                if (listener != null) {
                    listener.onResult(success);
                }
            }
        });

        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, result);
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute");
    }

    @Override
    protected void onProgressUpdate(Void... values) {}
}