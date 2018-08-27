/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.domain;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.muzima.utils.StringUtils;

public class Credentials {

    private final String serverUrl;
    private final String userName;
    private final String password;

    public Credentials(Context context) {
        this(PreferenceManager.getDefaultSharedPreferences(context));
    }

    private Credentials(SharedPreferences preferences) {
        this(preferences.getString("serverPreference", StringUtils.EMPTY),
                preferences.getString("usernamePreference", StringUtils.EMPTY),
                preferences.getString("passwordPreference", StringUtils.EMPTY));
    }

    public Credentials(String serverUrl, String userName, String password) {
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.password = password;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String[] getCredentialsArray() {
        String[] result = new String[3];
        result[0] = userName;
        result[1] = password;
        result[2] = serverUrl;
        return result;
    }

    public boolean isEmpty() {
        return getPassword().trim().length() == 0;
    }
}
