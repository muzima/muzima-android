package com.muzima.view.forms;

import android.support.v4.app.FragmentActivity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class UploadFormIntent extends SyncIntent {
    public UploadFormIntent(FragmentActivity activity) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_UPLOAD_FORMS);
    }
}
