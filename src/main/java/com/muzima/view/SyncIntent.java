package com.muzima.view;

import android.app.Activity;
import android.content.Intent;
import com.muzima.domain.Credentials;
import com.muzima.service.DataSyncService;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;

public class SyncIntent extends Intent {
    private final Activity activity;

    public SyncIntent(Activity activity) {
        super(activity, DataSyncService.class);
        this.activity = activity;
        putExtra(CREDENTIALS, new Credentials(activity).getCredentialsArray());
    }

    public void start() {
        activity.startService(this);
    }
}