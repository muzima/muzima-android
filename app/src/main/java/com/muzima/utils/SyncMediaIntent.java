package com.muzima.utils;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_MEDIA;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

import android.content.Context;

import com.muzima.view.SyncIntent;

public class SyncMediaIntent extends SyncIntent {
    public SyncMediaIntent(Context context){
        super(context);
        putExtra(SYNC_TYPE, SYNC_MEDIA);
    }
}
