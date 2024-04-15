package com.muzima.utils;

import android.content.Context;

import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.view.SyncIntent;

public class DownloadAndDeleteCohortsBasedOnConfigChangesIntent extends SyncIntent {
    public DownloadAndDeleteCohortsBasedOnConfigChangesIntent(Context context, SetupConfigurationTemplate configBeforeConfigUpdate) {
        super(context);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_PATIENT_FULL_PATIENT_DATA_BASED_ON_COHORT_CHANGES_IN_CONFIG);
        putExtra(Constants.DataSyncServiceConstants.CONFIG_BEFORE_UPDATE, configBeforeConfigUpdate);
    }
}
