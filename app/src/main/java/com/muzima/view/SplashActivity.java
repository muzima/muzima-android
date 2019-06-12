package com.muzima.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.messaging.CreateProfileActivity;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.IdentityKeyUtil;
import com.muzima.messaging.crypto.MasterSecret;
import com.muzima.messaging.crypto.MasterSecretUtil;
import com.muzima.messaging.twofactoraunthentication.RegistrationActivity;
import com.muzima.messaging.utils.Util;
import com.muzima.service.KeyCachingService;
import com.muzima.utils.VersionTracker;
import com.muzima.view.login.LoginActivity;

import java.io.IOException;

public class SplashActivity extends AppCompatActivity {
    Boolean isDisclaimerAccepted = false;
    Boolean isSignalUserAccountVerified = false;
    TextSecurePreferences textSecurePreferences;
    GoogleCloudMessaging gcm;
    private final String PREFERENCES_NAME = "SecureSMS-Preferences";
    private KeyCachingService keyCachingService;
    private MasterSecret masterSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (TextSecurePreferences.getGcmRegistrationId(SplashActivity.this) != null) {
            Log.e("TextSecurePreferences","TextSecurePreferences.getGcmRegistrationId(SplashActivity.this) != null");
        } else {
            new RegisterGoogleCloudMessagingAsyncTask().execute();
        }

        generateMasterSecret();

        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        isDisclaimerAccepted = settings.getBoolean(disclaimerKey, false);
        isSignalUserAccountVerified = settings.getBoolean(getString(R.string.preference_user_verified),false);
        Log.i("Disclaimer", "Disclaimer is accepted: " + isDisclaimerAccepted);
        if (!isDisclaimerAccepted) {
            Intent intent = new Intent(SplashActivity.this, DisclaimerActivity.class);
            startActivity(intent);
            finish();
        } else {
            if (!isSignalUserAccountVerified) {
                Intent intent = new Intent(SplashActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(this, LoginActivity.class);
                if (new Credentials(this).isEmpty()) {
                    intent.putExtra(LoginActivity.isFirstLaunch, false);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }
    }

    private void initializeResources() {
        new SecretGenerator().execute(MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isSignalUserAccountVerified) {
            initializeResources();
        } else if (isSignalUserAccountVerified && isDisclaimerAccepted) {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }


    private class RegisterGoogleCloudMessagingAsyncTask extends AsyncTask<Void,Void,Void> {
        private String REGISTRATION_ID = "312334754206";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e(getClass().getSimpleName(),"Async<~> --- registering device...");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (TextSecurePreferences.isGcmDisabled(getApplicationContext()))
                throw new AssertionError("Gcm disabled");

            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
            if (result != ConnectionResult.SUCCESS) {
                Log.e(getClass().getSimpleName(),"Gcm Registration Failed!!");
            } else {
                String gcmId = null;
                try {
                    gcmId = GoogleCloudMessaging.getInstance(getApplicationContext())
                            .register(REGISTRATION_ID);
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(),"Unable to register device "+e.getMessage());
                }

                TextSecurePreferences.setGcmRegistrationId(getApplicationContext(), gcmId);
                TextSecurePreferences.setGcmRegistrationIdLastSetTime(getApplicationContext(), System.currentTimeMillis());
                TextSecurePreferences.setWebsocketRegistered(getApplicationContext(), true);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.e(getClass().getSimpleName(),"Async<~> --- registration complete...");
            if (TextSecurePreferences.getGcmRegistrationId(SplashActivity.this) != null){
                initializeResources();
            }else
                Toast.makeText(SplashActivity.this,"Unable to register device, check your connection and try again.",Toast.LENGTH_LONG).show();
        }
    }

    private void  startNextActivity() {
        if (isDisclaimerAccepted) {
            startActivity(new Intent(SplashActivity.this, RegistrationActivity.class));
        } else
            startActivity(new Intent(SplashActivity.this, DisclaimerActivity.class));
    }

    private boolean generateMasterSecret() {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_NAME, 0);
        String salt = settings.getString("mac_salt", null);
        if (salt == null) {
            MasterSecretUtil.generateMasterSecret(getApplicationContext(), MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
        }

        return true;
    }

    private class SecretGenerator extends AsyncTask<String, Void, Void> {
        private MasterSecret masterSecret;

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "Generating Identity Keys", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Void doInBackground(String... params) {
            String passphrase = params[0];
            masterSecret = MasterSecretUtil.generateMasterSecret(SplashActivity.this,
                    passphrase);

            MasterSecretUtil.generateAsymmetricMasterSecret(SplashActivity.this, masterSecret);
            IdentityKeyUtil.generateIdentityKeys(SplashActivity.this);
            VersionTracker.updateLastSeenVersion(SplashActivity.this);

            TextSecurePreferences.setLastExperienceVersionCode(SplashActivity.this, Util.getCurrentApkReleaseVersion(SplashActivity.this));
            TextSecurePreferences.setPasswordDisabled(SplashActivity.this, true);
            TextSecurePreferences.setReadReceiptsEnabled(SplashActivity.this, true);
            TextSecurePreferences.setTypingIndicatorsEnabled(SplashActivity.this, true);

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            setMasterSecret(masterSecret);
        }
    }

    protected void setMasterSecret(MasterSecret masterSecret) {
        this.masterSecret = masterSecret;
        Intent bindIntent = new Intent(this, KeyCachingService.class);
        startService(bindIntent);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            keyCachingService = ((KeyCachingService.KeySetBinder) service).getService();
            keyCachingService.setMasterSecret(masterSecret);

            SplashActivity.this.unbindService(SplashActivity.this.serviceConnection);

            masterSecret = null;
            cleanup();

            Intent nextIntent = getIntent().getParcelableExtra("next_intent");
            if (nextIntent != null) {
                try {
                    startActivity(nextIntent);
                } catch (java.lang.SecurityException e) {
                    Log.w(getClass().getSimpleName(), "Access permission not passed from SplashActivity, retry sharing.");
                }
            }
            finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            keyCachingService = null;
        }
    };

    protected void cleanup() {
        System.gc();
    }
}