package hcmut.hoanganh.sunshine.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import hcmut.hoanganh.sunshine.sync.SunshineAuthenticator;

/**
 * Created by H.Anh on 25/02/2015.
 */
public class SunshineAuthenticationService extends Service {

    private SunshineAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();

        mAuthenticator = new SunshineAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

}
