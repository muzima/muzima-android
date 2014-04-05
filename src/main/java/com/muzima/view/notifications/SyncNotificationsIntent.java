package com.muzima.view.notifications;

import android.app.Activity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.COHORT_IDS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_NOTIFICATIONS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;
import static com.muzima.utils.Constants.NotificationStatusConstants.RECEIVER_UUID;

public class SyncNotificationsIntent extends SyncIntent {
    public SyncNotificationsIntent(Activity activity, String receiverUuid, String[] downloadedCohortsArray) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_NOTIFICATIONS);
        putExtra(RECEIVER_UUID, receiverUuid);
        putExtra(COHORT_IDS, downloadedCohortsArray);
    }
}
