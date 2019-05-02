/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.support.v4.app.FragmentActivity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS;

public class UploadFormIntent extends SyncIntent {
    public UploadFormIntent(FragmentActivity activity) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_UPLOAD_FORMS);
    }
}
