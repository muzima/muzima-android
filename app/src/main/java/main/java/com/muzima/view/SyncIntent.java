/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.content.Context;
import android.content.Intent;
import com.muzima.domain.Credentials;
import com.muzima.service.DataSyncService;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;

public class SyncIntent extends Intent {
    private final Context context;

    public SyncIntent(Context context) {
        super(context, DataSyncService.class);
        this.context = context;
        putExtra(CREDENTIALS, new Credentials(context).getCredentialsArray());
    }

    public void start() {
        context.startService(this);
    }
}