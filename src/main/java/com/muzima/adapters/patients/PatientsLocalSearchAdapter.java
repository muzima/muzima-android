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
import android.widget.ImageView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.User;
import com.muzima.controller.NotificationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class PatientsLocalSearchAdapter extends ListAdapter<Patient> {
    private static final String TAG = "PatientsLocalSearchAdapter";
    public static final String SEARCH = "search";
    private final PatientAdapterHelper patientAdapterHelper;
    private PatientController patientController;
    private final String cohortId;
    private Context context;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public PatientsLocalSearchAdapter(Context context, int textViewResourceId,
                                      PatientController patientController, String cohortId) {
        super(context, textViewResourceId);
        this.context = context;
        this.patientController = patientController;
        this.cohortId = cohortId;
        this.patientAdapterHelper = new PatientAdapterHelper(context, textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return patientAdapterHelper.createPatientRow(getItem(position), convertView, parent, getContext());
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(cohortId);
    }

    public void search(String text) {
        new BackgroundQueryTask().execute(text, SEARCH);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }


    private class BackgroundQueryTask extends AsyncTask<String, Void, List<Patient>> {

        @Override
        protected void onPreExecute() {
            patientAdapterHelper.onPreExecute(backgroundListQueryTaskListener);
        }

        @Override
        protected List<Patient> doInBackground(String... params) {
            List<Patient> patients = null;
            if (isSearch(params)) {
                try {
                    return patientController.searchPatientLocally(params[0], cohortId);
                } catch (PatientController.PatientLoadException e) {
                    Log.w(TAG, String.format("Exception occurred while searching patients for %s search string." , params[0]), e);
                }
            }

            String cohortUuid = params[0];
            try {
                if (cohortUuid != null) {
                    patients = patientController.getPatients(cohortUuid);
                } else {
                    patients = patientController.getAllPatients();
                }
            } catch (PatientController.PatientLoadException e) {
                Log.w(TAG, "Exception occurred while fetching patients", e);
            }
            return patients;
        }

        private boolean isSearch(String[] params) {
            return params.length == 2 && SEARCH.equals(params[1]);
        }

        @Override
        protected void onPostExecute(List<Patient> patients) {
            patientAdapterHelper.onPostExecute(patients, PatientsLocalSearchAdapter.this, backgroundListQueryTaskListener);
        }
    }
}
