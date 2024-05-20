/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import androidx.fragment.app.FragmentActivity;
import com.muzima.view.SyncIntent;

import android.content.Context;

import com.muzima.utils.Constants;

public class SyncFormTemplateIntent extends SyncIntent {
    public SyncFormTemplateIntent(FragmentActivity activity, String[] selectedFormsArray) {
        super(activity);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_TEMPLATES);
        putExtra(Constants.DataSyncServiceConstants.FORM_IDS, selectedFormsArray);
    }

    public SyncFormTemplateIntent(Context context, String[] selectedFormsArray) {
        super(context);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_TEMPLATES);
        putExtra(Constants.DataSyncServiceConstants.FORM_IDS, selectedFormsArray);
    }
}
