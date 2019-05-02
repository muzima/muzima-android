/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.app.Activity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants;

public class SyncPatientDataIntent extends SyncIntent {
    public SyncPatientDataIntent(Activity activity, String[] selectedCohortsArray) {
        super(activity);
        putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_PATIENTS_FULL_DATA);
        putExtra(DataSyncServiceConstants.COHORT_IDS, selectedCohortsArray);
    }
}
