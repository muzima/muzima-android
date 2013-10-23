package com.muzima.view.forms;

import android.support.v4.app.FragmentActivity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;

public class SyncFormTemplateIntent extends SyncIntent {
    public SyncFormTemplateIntent(FragmentActivity activity, String[] selectedFormsArray) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_TEMPLATES);
        putExtra(FORM_IDS, selectedFormsArray);
    }
}
