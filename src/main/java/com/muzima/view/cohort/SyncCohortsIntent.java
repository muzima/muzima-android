package com.muzima.view.cohort;

import android.app.Activity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_COHORTS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class SyncCohortsIntent extends SyncIntent {
    public SyncCohortsIntent(Activity activity) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_COHORTS);
    }
}
