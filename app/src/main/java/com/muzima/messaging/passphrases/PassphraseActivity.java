package com.muzima.messaging.passphrases;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.muzima.messaging.BaseActionBarActivity;
import com.muzima.messaging.crypto.MasterSecret;
import com.muzima.service.KeyCachingService;

public abstract class PassphraseActivity extends BaseActionBarActivity {

    private static final String TAG = PassphraseActivity.class.getSimpleName();

    private KeyCachingService keyCachingService;
    private MasterSecret masterSecret;

    protected void setMasterSecret(MasterSecret masterSecret) {
        this.masterSecret = masterSecret;
        Intent bindIntent = new Intent(this, KeyCachingService.class);
        startService(bindIntent);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    protected abstract void cleanup();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            keyCachingService = ((KeyCachingService.KeySetBinder)service).getService();
            keyCachingService.setMasterSecret(masterSecret);

            PassphraseActivity.this.unbindService(PassphraseActivity.this.serviceConnection);

            masterSecret = null;
            cleanup();

            Intent nextIntent = getIntent().getParcelableExtra("next_intent");
            if (nextIntent != null) {
                try {
                    startActivity(nextIntent);
                } catch (java.lang.SecurityException e) {
                    Log.w(TAG, "Access permission not passed from PassphraseActivity, retry sharing.");
                }
            }
            finish();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            keyCachingService = null;
        }
    };
}
