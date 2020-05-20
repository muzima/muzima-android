package com.muzima.view.setupconfiguration;

import android.content.Context;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_SETUP_CONFIGURATION_TEMPLATES;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class SyncSetupConfigurationTemplates extends SyncIntent {
    public SyncSetupConfigurationTemplates(Context context) {
        super(context);
        putExtra(SYNC_TYPE, SYNC_SETUP_CONFIGURATION_TEMPLATES);
    }
}
