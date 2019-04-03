/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import com.muzima.R;
import com.muzima.domain.Credentials;

public class CredentialsPreferenceService extends PreferenceService {

    private final SharedPreferences settings;

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
