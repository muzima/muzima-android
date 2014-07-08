package com.muzima.view.forms;

import android.support.v4.app.FragmentActivity;
import com.muzima.utils.Constants;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants;

public class SyncFormTemplateIntent extends SyncIntent {
    public SyncFormTemplateIntent(FragmentActivity activity, String[] selectedFormsArray) {
        super(activity);
        putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_TEMPLATES);
        putExtra(DataSyncServiceConstants.FORM_IDS, selectedFormsArray);
    }
}
