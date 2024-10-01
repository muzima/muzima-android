/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import static com.muzima.utils.DeviceDetailsUtil.generatePseudoDeviceId;

import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.service.NotificationTokenService;
import com.muzima.utils.MuzimaPreferences;

import java.io.IOException;

public class FCMTokenController {
    private final NotificationTokenService notificationTokenService;
    private final MuzimaApplication muzimaApplication;

    public FCMTokenController(NotificationTokenService notificationTokenService, MuzimaApplication muzimaApplication) {
        this.notificationTokenService = notificationTokenService;
        this.muzimaApplication = muzimaApplication;
    }

    public void sendTokenToServer() throws IOException {
        android.content.Context context = muzimaApplication.getApplicationContext();
        boolean notificationSetting = muzimaApplication.getMuzimaSettingController().isPushNotificationsEnabled();
        String appTokenKeySynced = context.getResources().getString(R.string.preference_app_token_synced);
        boolean tokenSynced = MuzimaPreferences.getBooleanPreference(context, appTokenKeySynced, false);

        if (notificationSetting && !tokenSynced) {
            String appTokenKey = context.getResources().getString(R.string.preference_app_token);
            String token = MuzimaPreferences.getStringPreference(context, appTokenKey, null);

            String pseudoDeviceId = generatePseudoDeviceId();
            String serial = "UNKNOWN";

            try {
                serial = Build.class.getField("SERIAL").get(null).toString();
                tokenSynced = notificationTokenService.sendTokenToServer(token, muzimaApplication.getAuthenticatedUser().getSystemId(), pseudoDeviceId, serial, Build.MODEL);
                if (tokenSynced) {
                    Resources resources = context.getResources();
                    String key = resources.getString(R.string.preference_app_token_synced);
                    MuzimaPreferences.setBooleanPreference(context, key, true);
                }
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while sending token to server", e);
            } catch (IllegalAccessException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while fetching serial ", e);
            } catch (NoSuchFieldException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while fetching serial ", e);
            }
        }
    }
}
