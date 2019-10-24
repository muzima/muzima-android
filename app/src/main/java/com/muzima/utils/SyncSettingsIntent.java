package com.muzima.utils;

import android.content.Context;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_SETTINGS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class SyncSettingsIntent extends SyncIntent {
    public SyncSettingsIntent(Context context){
        super(context);
        putExtra(SYNC_TYPE, SYNC_SETTINGS);
    }
}