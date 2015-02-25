package hcmut.hoanganh.sunshine.service;

import android.app.Service;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.os.IBinder;

import hcmut.hoanganh.sunshine.adapter.SunshineSyncAdapter;

/**
 * Created by H.Anh on 25/02/2015.
 */
public class SunshineSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    private static SunshineSyncAdapter sSunshineSyncAdapter;

    @Override
    public void onCreate() {
        super.onCreate();

        synchronized (sSyncAdapterLock) {
            if (sSunshineSyncAdapter == null) {
                sSunshineSyncAdapter = new SunshineSyncAdapter(this, true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSunshineSyncAdapter.getSyncAdapterBinder();
    }

}
