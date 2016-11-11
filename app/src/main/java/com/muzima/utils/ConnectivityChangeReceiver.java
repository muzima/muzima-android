package com.muzima.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.User;
import com.muzima.scheduler.RealTimeFormUploader;

/**
 * Created by shwethathammaiah on 07/10/14.
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectivityChangeReceiver";

    public ConnectivityChangeReceiver(){
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.i(TAG,"Connectivity change receiver triggered.");
        if (intent.getExtras() != null) {

            User authenticatedUser = ((MuzimaApplication) context.getApplicationContext()).getAuthenticatedUser();
            if (authenticatedUser != null) {
                Log.i(TAG,"Device got connected to network. Trying to start Muzima Real time Sync of completed forms.");
                RealTimeFormUploader.getInstance().uploadAllCompletedForms(context.getApplicationContext());
            }
        }
    }
}