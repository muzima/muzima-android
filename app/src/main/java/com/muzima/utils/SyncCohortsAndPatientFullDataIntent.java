package com.muzima.utils;

import android.app.Activity;
import android.content.Context;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_COHORTS_AND_ALL_PATIENTS_FULL_DATA;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class SyncCohortsAndPatientFullDataIntent extends SyncIntent {
    public SyncCohortsAndPatientFullDataIntent(Context context) {
        super(context);
        putExtra(SYNC_TYPE, SYNC_COHORTS_AND_ALL_PATIENTS_FULL_DATA);
    }
}