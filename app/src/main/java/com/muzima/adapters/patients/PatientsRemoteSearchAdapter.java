/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.patients;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.domain.Credentials;
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;
import com.muzima.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

public class PatientsRemoteSearchAdapter extends ListAdapter<Patient> {
    private final PatientAdapterHelper patientAdapterHelper;
    private final PatientController patientController;
    private final String searchString;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public PatientsRemoteSearchAdapter(Context context, int textViewResourceId, PatientController patientController,
                                       String searchString) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        this.searchString = searchString;
        this.patientAdapterHelper = new PatientAdapterHelper(context, textViewResourceId);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return patientAdapterHelper.createPatientRow(getItem(position), convertView, parent, getContext());
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    @Override
    public void reloadData() {
        new ServerSearchBackgroundTask().execute(searchString);
    }

    private class ServerSearchBackgroundTask extends AsyncTask<String, Void, Object> {
        @Override
        protected void onPreExecute() {
            patientAdapterHelper.onPreExecute(backgroundListQueryTaskListener);

        }

        @Override
        protected void onPostExecute(Object patientsObject) {
            List<Patient> patients = (List<Patient>)patientsObject;
            patientAdapterHelper.onPostExecute(patients, PatientsRemoteSearchAdapter.this, backgroundListQueryTaskListener);
        }

        @Override
        protected void onCancelled(Object result){
            if(result instanceof SERVER_CONNECTIVITY_STATUS){
                patientAdapterHelper.onNetworkError((SERVER_CONNECTIVITY_STATUS)result,backgroundListQueryTaskListener);
            } else {
                int authenticateResult = (int) result;
                patientAdapterHelper.onAuthenticationError(authenticateResult, backgroundListQueryTaskListener);
            }
        }

        @Override
        protected Object doInBackground(String... strings) {
            MuzimaApplication applicationContext = (MuzimaApplication) getContext();

            Credentials credentials = new Credentials(getContext());
            try {
                SERVER_CONNECTIVITY_STATUS serverStatus = NetworkUtils.getServerStatus(getContext(), credentials.getServerUrl());
                if(serverStatus == SERVER_CONNECTIVITY_STATUS.SERVER_ONLINE) {
                    int authenticateResult = applicationContext.getMuzimaSyncService().authenticate(credentials.getCredentialsArray());
                    if (authenticateResult == SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                        return patientController.searchPatientOnServer(strings[0]);
                    } else {
                        cancel(true);
                        return authenticateResult;
                    }
                }else {
                        cancel(true);
                        return serverStatus;
                }

            } catch (Throwable t) {
                Log.e(getClass().getSimpleName(), "Error while searching for patient in the server.", t);
            } finally {
                applicationContext.getMuzimaContext().closeSession();
            }
            Log.e(getClass().getSimpleName(), "Authentication failure !! Returning empty patient list");
            return new ArrayList<Patient>();
        }
    }
}
