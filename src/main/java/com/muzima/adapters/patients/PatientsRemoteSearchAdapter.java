/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.patients;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.domain.Credentials;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

public class PatientsRemoteSearchAdapter extends ListAdapter<Patient> {
    private static final String TAG = "PatientsRemoteSearchAdapter";
    private PatientAdapterHelper patientAdapterHelper;
    private PatientController patientController;
    private String searchString;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public PatientsRemoteSearchAdapter(Context context, int textViewResourceId, PatientController patientController,
                                       String searchString) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        this.searchString = searchString;
        this.patientAdapterHelper = new PatientAdapterHelper(context, textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return patientAdapterHelper.createPatientRow(getItem(position), convertView, parent, getContext());
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    @Override
    public void reloadData() {
        new ServerSearchBackgroundTask().execute(searchString);
    }

    private class ServerSearchBackgroundTask extends AsyncTask<String, Void, List<Patient>> {
        @Override
        protected void onPreExecute() {
            patientAdapterHelper.onPreExecute(backgroundListQueryTaskListener);

        }

        @Override
        protected void onPostExecute(List<Patient> patients) {
            patientAdapterHelper.onPostExecute(patients, PatientsRemoteSearchAdapter.this, backgroundListQueryTaskListener);
        }

        @Override
        protected List<Patient> doInBackground(String... strings) {
            MuzimaApplication applicationContext = (MuzimaApplication) getContext();

            Credentials credentials = new Credentials(getContext());
            try {
                int authenticateResult = applicationContext.getMuzimaSyncService().authenticate(credentials.getCredentialsArray());
                if (authenticateResult == SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                    return patientController.searchPatientOnServer(strings[0]);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error while searching for patient in the server.", t);
            } finally {
                applicationContext.getMuzimaContext().closeSession();
            }
            Log.e(TAG, "Authentication failure !! Returning empty patient list");
            return new ArrayList<Patient>();
        }
    }
}
