package com.raffler.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Ghost on 6/9/2017.
 */

public class SyncService extends Service {

    @Override
    public void onCreate() {
        synchronized (_sync_adapter_lock) {
            if (_sync_adapter == null)
                _sync_adapter = new SyncAdapter(getApplicationContext(), false);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _sync_adapter.getSyncAdapterBinder();
    }

    private static final Object _sync_adapter_lock = new Object();
    private static SyncAdapter _sync_adapter = null;
}
