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
    private AsyncTask<String, List<Patient>, List<Patient>> backgroundQueryTask;
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
        if(backgroundQueryTask != null){
            backgroundQueryTask.cancel(true);
        }
        backgroundQueryTask = new BackgroundQueryTask().execute(cohortId);
    }

    public void search(String text) {
        if(backgroundQueryTask != null){
            backgroundQueryTask.cancel(true);
        }
        backgroundQueryTask = new BackgroundQueryTask().execute(text, SEARCH);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public void cancelBackgroundTask(){
        backgroundQueryTask.cancel(true);
    }


    private class BackgroundQueryTask extends AsyncTask<String, List<Patient>, List<Patient>> {

        @Override
        protected void onPreExecute() {
            patientAdapterHelper.onPreExecute(backgroundListQueryTaskListener);
            PatientsLocalSearchAdapter.this.clear();
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
                    publishProgress(patients);
                } else {
                    int pageSize = 10;
                    int patientCount = patientController.getTotalPatientsCount();
                    if(patientCount <= 10){
                        patients = patientController.getAllPatients();
                    } else {
                        int pages = new Double(Math.ceil(patientCount / pageSize)).intValue();
                        List<Patient> temp = null;
                        for (int page = 1; page <= pages; page++) {
                            if(!isCancelled()) {
                                if (patients == null) {
                                    patients = patientController.getAllPatients(page, pageSize);
                                    if (patients != null) {
                                        publishProgress(patients);
                                    }
                                } else {
                                    temp = patientController.getAllPatients(page, pageSize);
                                    if (temp != null) {
                                        patients.addAll(temp);
                                        publishProgress(temp);
                                    }
                                }
                            }
                        }
                    }
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
        @Override
        protected void onProgressUpdate(List<Patient>... patients) {
            for (List<Patient> patientList : patients) {
                patientAdapterHelper.onProgressUpdate(patientList, PatientsLocalSearchAdapter.this, backgroundListQueryTaskListener);
            }
        }
    }
}