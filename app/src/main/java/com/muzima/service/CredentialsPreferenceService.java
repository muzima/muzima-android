/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.muzima.R;
import com.muzima.api.context.MuzimaContext;
import com.muzima.api.service.UserService;
import com.muzima.domain.Credentials;
import com.muzima.utils.MuzimaPreferences;

import java.io.IOException;

public class CredentialsPreferenceService extends PreferenceService {

    public CredentialsPreferenceService(Context context) {
        super(context);
    }

    public void saveCredentials(Credentials credentials) {
        try {
            Resources resources = context.getResources();
            String usernameKey = resources.getString(R.string.preference_username);
            String passwordKey = resources.getString(R.string.preference_password);
            String serverKey = resources.getString(R.string.preference_server);

            MuzimaPreferences.getSecureSharedPreferences(context).edit()
                    .putString(usernameKey, credentials.getUserName())
                    .putString(passwordKey, credentials.getPassword())
                    .putString(serverKey, credentials.getServerUrl())
                    .commit();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error getting secure shared preferences");
        }
    }

    public void deleteUserData(MuzimaContext muzimaContext){
        try {
            UserService userService = muzimaContext.getUserService();
            userService.deleteAllUsers();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),"An exception was encountered while deleting users ",e);
        }
    }

}
