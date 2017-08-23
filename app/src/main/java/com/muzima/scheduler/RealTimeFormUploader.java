/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.scheduler;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.forms.RealTimeUploadFormIntent;

public class RealTimeFormUploader {
    private static RealTimeFormUploader INSTANCE = new RealTimeFormUploader();
    private static final String TAG = "RealTimeFormUploader";

    public static RealTimeFormUploader getInstance() {
        return INSTANCE;
    }

    private RealTimeFormUploader() {

    }

    public  void uploadAllCompletedForms(android.content.Context applicationContext){
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        if(preferences.getBoolean("realTimeSyncPreference", false)){
            uploadAllFormsInBackgroundService(applicationContext);
        }
    }

    private void uploadAllFormsInBackgroundService(android.content.Context applicationContext) {
        try {
            FormController formController = ((MuzimaApplication) applicationContext).getFormController();
            if(formController.countAllCompleteForms() > 0 && NetworkUtils.isConnectedToNetwork(applicationContext)){
                new RealTimeUploadFormIntent(applicationContext).start();
            }
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Error while trying to access completed form data", e);
        }
    }
}
