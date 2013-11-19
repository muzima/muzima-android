package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.muzima.R;
import com.muzima.domain.Credentials;

public class CredentialsPreferenceService extends PreferenceService {

    private SharedPreferences settings;

    public CredentialsPreferenceService(Context context) {
        super(context);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void saveCredentials(Credentials credentials) {
        Resources resources = context.getResources();
        String usernameKey = resources.getString(R.string.preference_username);
        String passwordKey = resources.getString(R.string.preference_password);
        String serverKey = resources.getString(R.string.preference_server);

        settings.edit()
                .putString(usernameKey, credentials.getUserName())
                .putString(passwordKey, credentials.getPassword())
                .putString(serverKey, credentials.getServerUrl())
                .commit();
    }
}
