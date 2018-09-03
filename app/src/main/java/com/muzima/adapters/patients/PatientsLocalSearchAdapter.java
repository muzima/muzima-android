/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
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
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;

import java.util.List;

public class PatientsLocalSearchAdapter extends ListAdapter<Patient> {
    private static final String SEARCH = "search";
    private final PatientAdapterHelper patientAdapterHelper;
    private final PatientController patientController;
    private final String cohortId;
    private AsyncTask<String, List<Patient>, List<Patient>> backgroundQueryTask;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public PatientsLocalSearchAdapter(Context context, int textViewResourceId,
                                      PatientController patientController, String cohortId) {
        super(context, textViewResourceId);
        Context context1 = context;
        this.patientController = patientController;
        this.cohortId = cohortId;
        this.patientAdapterHelper = new PatientAdapterHelper(context, textViewResourceId);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return patientAdapterHelper.createPatientRow(getItem(position), convertView, parent, getContext());
    }

    @Override
    public void reloadData() {
        cancelBackgroundTask();
        backgroundQueryTask = new BackgroundQueryTask().execute(cohortId);
    }

    public void search(String text) {
        cancelBackgroundTask();
        if(StringUtils.isEmpty(text)) {
            reloadData();
        } else {
            backgroundQueryTask = new BackgroundQueryTask().execute(text, SEARCH);
        }
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public void cancelBackgroundTask(){
        if(backgroundQueryTask != null){
            backgroundQueryTask.cancel(true);
        }
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
                    Log.w(getClass().getSimpleName(), String.format("Exception occurred while searching patients for %s search string." , params[0]), e);
                }
            }

            String cohortUuid = params[0];
            try {
                int pageSize = Constants.PATIENT_LOAD_PAGE_SIZE;
                if (cohortUuid != null) {
                    int patientCount = patientController.countPatients(cohortUuid);
                    if(patientCount <= pageSize){
                        patients = patientController.getPatients(cohortUuid);
                    } else {
                        int pages = new Double(Math.ceil(patientCount / pageSize)).intValue();
                        List<Patient> temp = null;
                        for (int page = 1; page <= pages; page++) {
                            if(!isCancelled()) {
                                if (patients == null) {
                                    patients = patientController.getPatients(cohortUuid, page, pageSize);
                                    if (patients != null) {
                                        publishProgress(patients);
                                    }
                                } else {
                                    temp = patientController.getPatients(cohortUuid, page, pageSize);
                                    if (temp != null) {
                                        patients.addAll(temp);
                                        publishProgress(temp);
                                    }
                                }
                            }
                        }
                    }

                } else {
                    int patientCount = patientController.countAllPatients();
                    if(patientCount <= pageSize){
                        patients = patientController.getAllPatients();
                    } else {
                        int pages = new Double(Math.ceil(patientCount / pageSize)).intValue();
                        List<Patient> temp = null;
                        for (int page = 1; page <= pages; page++) {
                            if(!isCancelled()) {
                                if (patients == null) {
                                    patients = patientController.getPatients(page, pageSize);
                                    if (patients != null) {
                                        publishProgress(patients);
                                    }
                                } else {
                                    temp = patientController.getPatients(page, pageSize);
                                    if (temp != null) {
                                        patients.addAll(temp);
                                        publishProgress(temp);
                                    }
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } catch (PatientController.PatientLoadException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while fetching patients", e);
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
        @SafeVarargs
        @Override
        protected final void onProgressUpdate(List<Patient>... patients) {
            for (List<Patient> patientList : patients) {
                patientAdapterHelper.onProgressUpdate(patientList, PatientsLocalSearchAdapter.this, backgroundListQueryTaskListener);
            }
        }
    }
}