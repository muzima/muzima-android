/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.scheduler;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Person;
import com.muzima.api.model.User;
import com.muzima.controller.FormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.utils.ProcessedTemporaryFormDataCleanUpIntent;
import com.muzima.utils.SyncCohortsAndPatientFullDataIntent;
import com.muzima.utils.SyncSettingsIntent;
import com.muzima.view.forms.SyncFormIntent;
import com.muzima.view.reports.SyncAllPatientReports;
import com.muzima.view.initialwizard.SyncSetupConfigurationTemplates;

@SuppressLint("NewApi")
public class MuzimaJobScheduler extends JobService {

    private MuzimaSyncService muzimaSynService;
    private String authenticatedUserUuid;
    private User authenticatedUser;
    private Person person;
    private boolean isAuthPerson = false;
    private MuzimaSettingController muzimaSettingController;

    @Override
    public void onCreate() {
        super.onCreate();
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        muzimaSettingController = muzimaApplication.getMuzimaSettingController();
        muzimaSynService = muzimaApplication.getMuzimaSyncService();
        authenticatedUser = muzimaApplication.getAuthenticatedUser();
        if (authenticatedUser != null){
            person = authenticatedUser.getPerson();

            if (person != null){
                authenticatedUserUuid = person.getUuid();
                isAuthPerson = true;
            }else{
                isAuthPerson = false;
            }

        }else {
            isAuthPerson = false;
            Log.i(getClass().getSimpleName(), "Authenticated user is not a person");
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {

        if (authenticatedUser == null || !isAuthPerson) {
            onStopJob(params);
        } else {
            //execute job
            Toast.makeText(getApplicationContext(), R.string.info_background_data_sync_started,Toast.LENGTH_LONG).show();
            handleBackgroundWork(params);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(getClass().getSimpleName(), "mUzima Job Service stopped" + params.getJobId());
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(getClass().getSimpleName(), "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(getClass().getSimpleName(), "Downloading messages in Job");
        return START_NOT_STICKY;
    }

    private void handleBackgroundWork(JobParameters parameters) {
        if (parameters == null) {
            Log.e(getClass().getSimpleName(), "Parameters for job is null");
        } else {
            new CohortsAndPatientFullDataSyncBackgroundTask().execute();
            new FormDataUploadBackgroundTask().execute();
            new ProcessedTemporaryFormDataCleanUpBackgroundTask().execute();
            new SyncSetupConfigTemplatesBackgroundTask().execute();
            new SyncSettinsBackgroundTask().execute();
            if(muzimaSettingController.isClinicalSummaryEnabled()) {
                new SyncAllPatientReportsBackgroundTask().execute();
            }
            new FormMetaDataSyncBackgroundTask().execute();
        }
    }

    private class  ProcessedTemporaryFormDataCleanUpBackgroundTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
           new ProcessedTemporaryFormDataCleanUpIntent(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class CohortsAndPatientFullDataSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (new WizardFinishPreferenceService(MuzimaJobScheduler.this).isWizardFinished()) {
                new SyncCohortsAndPatientFullDataIntent(getApplicationContext()).start();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormDataUploadBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (new WizardFinishPreferenceService(getApplicationContext()).isWizardFinished()) {
                RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(),true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormMetaDataSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                if (new WizardFinishPreferenceService(context).isWizardFinished() &&
                        !((MuzimaApplication) context).getFormController().isFormWithPatientDataAvailable(context)) {

                    new SyncFormIntent(getApplicationContext()).start();
                } else {
                    Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not sync form metadata. Incomplete/unsyched forms exist");
                }
            } catch (FormController.FormFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not sync form metadata",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncSettinsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncSettingsIntent(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncAllPatientReportsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncAllPatientReports(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncSetupConfigTemplatesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncSetupConfigurationTemplates(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}
