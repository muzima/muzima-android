package com.muzima.view.notifications;

import android.app.Activity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_NOTIFICATIONS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class SyncPatientNotificationsIntent extends SyncIntent {
    public SyncPatientNotificationsIntent(Activity activity) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_NOTIFICATIONS);
    }
}
