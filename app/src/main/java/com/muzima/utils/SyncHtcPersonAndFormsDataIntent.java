package com.muzima.utils;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_HTC_PERSONS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

import android.content.Context;

import com.muzima.api.model.User;
import com.muzima.view.SyncIntent;

public class SyncHtcPersonAndFormsDataIntent extends SyncIntent {
    public SyncHtcPersonAndFormsDataIntent(Context context, User authenticatedUser) {
        super(context);
        putExtra(SYNC_TYPE, SYNC_HTC_PERSONS);
        putExtra("USER_UUID", authenticatedUser.getUuid());
    }
}
