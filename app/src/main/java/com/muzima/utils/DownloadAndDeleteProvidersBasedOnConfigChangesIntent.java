package com.muzima.utils;

import android.content.Context;

import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.view.SyncIntent;

public class DownloadAndDeleteProvidersBasedOnConfigChangesIntent extends SyncIntent {
    public DownloadAndDeleteProvidersBasedOnConfigChangesIntent(Context context, SetupConfigurationTemplate configBeforeConfigUpdate) {
        super(context);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_PROVIDERS_BASED_ON_CHANGES_IN_CONFIG);
        putExtra(Constants.DataSyncServiceConstants.CONFIG_BEFORE_UPDATE, configBeforeConfigUpdate);
    }
}
