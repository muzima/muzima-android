package com.muzima.utils;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CONFIG_BEFORE_UPDATE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_MEDIA_CATEGORIES;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

import android.content.Context;

import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.view.SyncIntent;

public class SyncMediaCategoryIntent extends SyncIntent{
    public SyncMediaCategoryIntent(Context context, SetupConfigurationTemplate configBeforeConfigUpdate){
        super(context);
        putExtra(SYNC_TYPE, SYNC_MEDIA_CATEGORIES);
        putExtra(CONFIG_BEFORE_UPDATE, configBeforeConfigUpdate);
    }
}
