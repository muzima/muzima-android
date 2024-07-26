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

import static com.muzima.utils.DeviceDetailsUtil.generatePseudoDeviceId;
import static com.muzima.utils.StringUtils.EMPTY;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.AppUsageLogs;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class ActiveConfigPreferenceService extends PreferenceService{

    private final SharedPreferences settings;
    private final MuzimaApplication application;

    public ActiveConfigPreferenceService(MuzimaApplication application){
        super(application.getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = application;
    }

    public String getActiveConfigUuid(){
        String key = context.getResources().getString(R.string.active_config_uuid);
        return settings.getString(key,EMPTY);
    }

    public void setActiveConfigUuid(String uuid){
        String key = context.getResources().getString(R.string.active_config_uuid);
        settings.edit().putString(key,uuid).apply();

        AppUsageLogs setupConfig = new AppUsageLogs();
        setupConfig.setUuid(UUID.randomUUID().toString());
        setupConfig.setLogKey(com.muzima.util.Constants.AppUsageLogs.SET_UP_CONFIG_UUID);
        setupConfig.setLogvalue(uuid);
        setupConfig.setUpdateDatetime(new Date());
        setupConfig.setLogSynced(false);

        String loggedInUser = application.getAuthenticatedUserId();
        setupConfig.setUserName(loggedInUser);

        String pseudoDeviceId = generatePseudoDeviceId();
        setupConfig.setDeviceId(pseudoDeviceId);
        try {
            application.getAppUsageLogsController().saveOrUpdateAppUsageLog(setupConfig);
        } catch (Throwable e) {
            Log.e(getClass().getSimpleName(), "Cannot save activeConfig app usage log", e);
        }
    }
}
