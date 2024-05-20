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

import com.muzima.utils.Constants;

public class UploadFormIntent extends SyncIntent {
    public UploadFormIntent(FragmentActivity activity) {
        super(activity);
        putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS);
    }
}
