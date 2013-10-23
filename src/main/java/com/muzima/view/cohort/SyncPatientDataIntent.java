package com.muzima.view.cohort;

import android.app.Activity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;

public class SyncPatientDataIntent extends SyncIntent {
    public SyncPatientDataIntent(Activity activity, String[] selectedCohortsArray) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_PATIENTS_FULL_DATA);
        putExtra(COHORT_IDS, selectedCohortsArray);
    }
}
