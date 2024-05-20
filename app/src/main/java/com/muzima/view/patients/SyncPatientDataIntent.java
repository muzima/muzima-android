/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.app.Activity;
import android.content.Context;

import com.muzima.view.SyncIntent;

import com.muzima.utils.Constants;

public class SyncPatientDataIntent extends SyncIntent {
    public SyncPatientDataIntent(Activity activity, String[] selectedCohortsArray) {
        super(activity);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA);
        putExtra(Constants.DataSyncServiceConstants.COHORT_IDS, selectedCohortsArray);
    }

    public SyncPatientDataIntent(Context context, String[] selectedCohortsArray) {
        super(context);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA);
        putExtra(Constants.DataSyncServiceConstants.COHORT_IDS, selectedCohortsArray);
    }
}
