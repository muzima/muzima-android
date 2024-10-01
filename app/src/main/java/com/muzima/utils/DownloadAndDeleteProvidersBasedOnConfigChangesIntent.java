/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CONFIG_BEFORE_UPDATE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_PROVIDERS_BASED_ON_CHANGES_IN_CONFIG;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

import android.content.Context;

import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.view.SyncIntent;

public class DownloadAndDeleteProvidersBasedOnConfigChangesIntent extends SyncIntent {
    public DownloadAndDeleteProvidersBasedOnConfigChangesIntent(Context context, SetupConfigurationTemplate configBeforeConfigUpdate) {
        super(context);
        putExtra(SYNC_TYPE, SYNC_PROVIDERS_BASED_ON_CHANGES_IN_CONFIG);
        putExtra(CONFIG_BEFORE_UPDATE, configBeforeConfigUpdate);
    }
}
