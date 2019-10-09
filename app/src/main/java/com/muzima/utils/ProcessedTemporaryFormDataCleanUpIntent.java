package com.muzima.utils;

import android.content.Context;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CLEAN_UP_PROCESSED_TEMPORARY_FORM_DATA;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class ProcessedTemporaryFormDataCleanUpIntent extends SyncIntent {
    public ProcessedTemporaryFormDataCleanUpIntent(Context context){
        super(context);
        putExtra(SYNC_TYPE, CLEAN_UP_PROCESSED_TEMPORARY_FORM_DATA);
    }
}
