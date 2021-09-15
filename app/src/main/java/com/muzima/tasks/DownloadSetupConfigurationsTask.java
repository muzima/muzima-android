/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.tasks;

import android.content.Context;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.controller.SetupConfigurationController;

import java.util.ArrayList;
import java.util.List;

public class DownloadSetupConfigurationsTask implements Runnable {
    private static final String TAG = "DownloadSetupConfigurat";
    private Context context;
    private SetupConfigurationCompletedCallback callback;

    public DownloadSetupConfigurationsTask(Context context, SetupConfigurationCompletedCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        List<SetupConfiguration> setupConfigurations = new ArrayList<>();
        try {
            ((MuzimaApplication) context.getApplicationContext()).getMuzimaSyncService().downloadSetupConfigurations();
            setupConfigurations = ((MuzimaApplication) context.getApplicationContext()).getSetupConfigurationController().getAllSetupConfigurations();
            Log.e(TAG, "#SetupConfigurations: " + setupConfigurations.size());
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
            Log.e(TAG, "Exception occurred while fetching the downloaded Setup Configurations", e);
        }
        callback.setupConfigDownloadCompleted(setupConfigurations);
    }

    public interface SetupConfigurationCompletedCallback {
        void setupConfigDownloadCompleted(List<SetupConfiguration> configurationList);
    }
}
