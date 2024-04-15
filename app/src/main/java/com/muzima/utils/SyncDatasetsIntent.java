package com.muzima.utils;

import android.content.Context;

import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.view.SyncIntent;

public class SyncDatasetsIntent extends SyncIntent {
    public SyncDatasetsIntent(Context context, SetupConfigurationTemplate configBeforeConfigUpdate){
        super(context);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_DATASETS);
        putExtra(Constants.DataSyncServiceConstants.CONFIG_BEFORE_UPDATE, configBeforeConfigUpdate);
    }
}
