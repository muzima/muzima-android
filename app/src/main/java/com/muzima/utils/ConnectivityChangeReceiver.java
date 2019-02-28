/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

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


    public ConnectivityChangeReceiver(){
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.i(getClass().getSimpleName(),"Connectivity change receiver triggered.");
        if (intent.getExtras() != null) {

            User authenticatedUser = ((MuzimaApplication) context.getApplicationContext()).getAuthenticatedUser();
            if (authenticatedUser != null) {
                Log.i(getClass().getSimpleName(),"Device got connected to network. Trying to start Muzima Real time Sync of completed forms.");
                RealTimeFormUploader.getInstance().uploadAllCompletedForms(context.getApplicationContext());
            }
        }
    }
}