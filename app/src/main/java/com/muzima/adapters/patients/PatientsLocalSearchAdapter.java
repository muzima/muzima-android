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
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;
import com.muzima.view.CheckedLinearLayout;

import java.util.ArrayList;
import java.util.List;

public class PatientsLocalSearchAdapter extends ListAdapter<Patient> {
    private static final String SEARCH = "search";
    private final PatientAdapterHelper patientAdapterHelper;
    private final PatientController patientController;
    private final String cohortId;
    private AsyncTask<String, List<Patient>, List<Patient>> backgroundQueryTask;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private View.OnClickListener onClickListener;

    private PatientListClickListener patientListClickListener;
    private List<String> selectedPatientsUuids;

    public PatientsLocalSearchAdapter(Context context, int textViewResourceId,
                                      PatientController patientController, String cohortId, MuzimaGPSLocation currentLocation) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        this.cohortId = cohortId;
        this.patientAdapterHelper = new PatientAdapterHelper(context, textViewResourceId, patientController);
        patientAdapterHelper.setCurrentLocation(currentLocation);
        selectedPatientsUuids = new ArrayList<>();
    }

    public void setPatientListLongClickListener(PatientListClickListener patientListClickListener) {
        this.patientListClickListener = patientListClickListener;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        convertView = patientAdapterHelper.createPatientRow(getItem(position), convertView, parent, getContext());

        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                toggleSelection(view, position);
                patientListClickListener.onItemLongClick();
                return true;
            }
        });

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                patientListClickListener.onItemClick(view, position);
            }
        });
        return convertView;
    }

    public void toggleSelection(View view, int position){
        CheckedLinearLayout checkedLinearLayout = (CheckedLinearLayout) view;
        checkedLinearLayout.toggle();
        boolean selected = checkedLinearLayout.isChecked();

        Patient clickedPatient = getItem(position);
        if (selected && !selectedPatientsUuids.contains(clickedPatient.getUuid())) {
            selectedPatientsUuids.add(clickedPatient.getUuid());
            checkedLinearLayout.setActivated(true);
        } else if (!selected && selectedPatientsUuids.contains(clickedPatient.getUuid())) {
            selectedPatientsUuids.remove(clickedPatient.getUuid());
            checkedLinearLayout.setActivated(false);
        }
    }

    public List<String> getSelectedPatientsUuids() {
        return selectedPatientsUuids;
    }

    @Override
    public void reloadData() {
        selectedPatientsUuids.clear();
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
            List<Patient> filteredPatients = null;

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
                        int pages = new Double(Math.ceil((float)patientCount / pageSize)).intValue();
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
                        int pages = new Double(Math.ceil((float)patientCount / pageSize)).intValue();
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
            List<String> tags = patientController.getSelectedTagUuids();
            filteredPatients = patientController.filterPatientByTags(patients,tags);
            return filteredPatients;
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

    public interface PatientListClickListener {

        void onItemLongClick();
        void onItemClick(View view, int position);
    }
}