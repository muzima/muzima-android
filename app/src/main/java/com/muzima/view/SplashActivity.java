package com.muzima.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.twofactoraunthentication.RegistrationActivity;
import com.muzima.view.login.LoginActivity;

import java.io.IOException;

public class SplashActivity extends AppCompatActivity {
    Boolean isDisclaimerAccepted = false;
    Boolean isSignalUserAccountVerified = false;
    TextSecurePreferences textSecurePreferences;
    GoogleCloudMessaging gcm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (TextSecurePreferences.getGcmRegistrationId(SplashActivity.this) != null) {
            Log.e("TextSecurePreferences","TextSecurePreferences.getGcmRegistrationId(SplashActivity.this) != null");
        } else {
            new RegisterGoogleCloudMessagingAsyncTask().execute();
        }

        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        isDisclaimerAccepted = settings.getBoolean(disclaimerKey, false);
        isSignalUserAccountVerified = settings.getBoolean(getString(R.string.signal_user_verified_preference),false);
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
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
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
            // if (TextSecurePreferences.isGcmDisabled(getApplicationContext())) return;
            Log.i(getClass().getSimpleName(), "Reregistering GCM...");
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
            if (result != ConnectionResult.SUCCESS) {
                //notifyGcmFailure();
            } else {
                String gcmId = null;
                try {
                    gcmId = GoogleCloudMessaging.getInstance(getApplicationContext())
                            .register(REGISTRATION_ID);
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(),"Unable to register device "+e.getMessage());
                }

                //textSecureAccountManager.setGcmId(Optional.of(gcmId));
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
                startNextActivity();
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
}