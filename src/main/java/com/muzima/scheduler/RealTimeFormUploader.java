package com.muzima.scheduler;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.forms.RealTimeUploadFormIntent;

/**
* Created by shwethathammaiah on 07/10/14.
*/
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
            if(formController.getAllCompleteFormsSize() > 0 && NetworkUtils.isConnectedToNetwork(applicationContext)){
                new RealTimeUploadFormIntent(applicationContext).start();
            }
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Error while trying to access completed form data", e);
        }
    }
}
