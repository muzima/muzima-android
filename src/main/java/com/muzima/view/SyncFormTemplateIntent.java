package com.muzima.view;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import com.muzima.domain.Credentials;
import com.muzima.service.DataSyncService;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;

public class SyncFormTemplateIntent extends Intent {
    private FragmentActivity activity;

    public SyncFormTemplateIntent(FragmentActivity activity, String[] selectedFormsArray) {
        this(activity, selectedFormsArray, new Credentials(activity).getCredentialsArray());
    }

    public SyncFormTemplateIntent(FragmentActivity activity, String[] selectedFormsArray, String[] credentialsArray) {
        super(activity, DataSyncService.class);
        this.activity = activity;
        putExtra(SYNC_TYPE, SYNC_TEMPLATES);
        putExtra(CREDENTIALS, credentialsArray);
        putExtra(FORM_IDS, selectedFormsArray);
    }

    public void start() {
        activity.startService(this);
    }
}
