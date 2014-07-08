package com.muzima.view.cohort;

import android.app.Activity;
import com.muzima.utils.Constants;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants;

public class SyncPatientDataIntent extends SyncIntent {
    public SyncPatientDataIntent(Activity activity, String[] selectedCohortsArray) {
        super(activity);
        putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_PATIENTS_FULL_DATA);
        putExtra(DataSyncServiceConstants.COHORT_IDS, selectedCohortsArray);
    }
}
