package com.muzima.view.forms;

import android.app.Activity;
import android.content.Context;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS;

/**
 * Created by shwethathammaiah on 06/10/14.
 */
public class RealTimeUploadFormIntent extends SyncIntent{

    public RealTimeUploadFormIntent(Context activity){
        super(activity);
        putExtra(SYNC_TYPE, SYNC_REAL_TIME_UPLOAD_FORMS);
    }
}
