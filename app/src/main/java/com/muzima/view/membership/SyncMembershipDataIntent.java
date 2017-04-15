/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.membership;

import android.app.Activity;

import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants;

public class SyncMembershipDataIntent extends SyncIntent {
    public SyncMembershipDataIntent(Activity activity, String[] selectedCohortsArray) {
        super(activity);
        putExtra(DataSyncServiceConstants.SYNC_TYPE,
                DataSyncServiceConstants.SYNC_COHORT_MEMBERSHIP_FULL_DATA);
        putExtra(DataSyncServiceConstants.COHORT_IDS, selectedCohortsArray);
    }
}
