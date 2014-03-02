package com.muzima.view.notifications;

import android.app.Activity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_NOTIFICATIONS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;
import static com.muzima.utils.Constants.NotificationStatusConstants.SENDER_UUID;

public class SyncNotificationsIntent extends SyncIntent {
    public SyncNotificationsIntent(Activity activity, String senderUuid) {
        super(activity);
        putExtra(SYNC_TYPE, SYNC_NOTIFICATIONS);
        putExtra(SENDER_UUID, senderUuid);
    }
}
