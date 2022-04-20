/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.patients;

import android.content.Context;
import android.util.Log;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PatientsLocalSearchAdapter extends PatientAdapterHelper implements MuzimaAsyncTask.OnProgressListener {
    private static final String SEARCH = "search";
    private final PatientController patientController;
    private final List<String> cohortUuids;
    private MuzimaAsyncTask<String, List<Patient>, List<Patient>> backgroundQueryTask;


    public PatientsLocalSearchAdapter(Context context, PatientController patientController,
                                      List<String> cohortUuids,
                                      MuzimaGPSLocation currentLocation) {
        super(context,patientController);
        this.patientController = patientController;
        if (cohortUuids != null){
            this.cohortUuids = cohortUuids;
        } else {
            this.cohortUuids = new ArrayList<>();
        }
        setCurrentLocation(currentLocation);
    }

    @Override
    public void reloadData() {
        cancelBackgroundTask();
        if(!cohortUuids.isEmpty() ) {
            backgroundQueryTask = new BackgroundQueryTask();
            backgroundQueryTask.execute(cohortUuids.toArray(new String[cohortUuids.size()]));
        } else {
            backgroundQueryTask = new BackgroundQueryTask();
            backgroundQueryTask.execute(StringUtils.EMPTY);
        }
    }

    public void search(String text) {
        cancelBackgroundTask();
        if(StringUtils.isEmpty(text)) {
            reloadData();
        } else {
            backgroundQueryTask = new BackgroundQueryTask();
            backgroundQueryTask.execute(text, SEARCH);
        }
    }

    public void filterByCohorts(List<String> cohortUuids) {
        cancelBackgroundTask();
        this.cohortUuids.clear();
        this.cohortUuids.addAll(cohortUuids);
        reloadData();
    }


    public void cancelBackgroundTask(){
        if(backgroundQueryTask != null){
            backgroundQueryTask.cancel();
        }
    }

    @Override
    public void onProgress(Object o) {
        try {
            onProgressUpdate((List<Patient>) o);
        } catch (ClassCastException e){
            Log.e(getClass().getSimpleName(),"Argument is not a patient list",e);
        }
    }

    private class BackgroundQueryTask extends MuzimaAsyncTask<String, List<Patient>, List<Patient>> {

        @Override
        protected void onPreExecute() {
            onPreExecuteUpdate();
            setOnProgressListener(PatientsLocalSearchAdapter.this);
        }

        @Override
        protected List<Patient> doInBackground(String... params) {
            List<Patient> patients = null;
            List<Patient> filteredPatients = null;

            if (isSearch(params)) {
                try {
                    if(cohortUuids.size() == 1)
                        return patientController.searchPatientLocally(params[0], cohortUuids.get(0));
                    else
                        return patientController.searchPatientLocally(params[0],null);
                } catch (PatientController.PatientLoadException e) {
                    Log.w(getClass().getSimpleName(), String.format("Exception occurred while searching patients for %s search string." , params[0]), e);
                }
            }

            try {
                int pageSize = Constants.PATIENT_LOAD_PAGE_SIZE;
                if (!cohortUuids.isEmpty()) {
                    for(String cohortUuid :cohortUuids) {
                        int patientCount = patientController.countPatients(cohortUuid);
                        List<Patient> temp = null;

                        if (patientCount <= pageSize) {
                            temp = patientController.getPatients(cohortUuid);
                            if(patients == null)
                                patients = temp;
                            else
                                patients.addAll(temp);
                            publishProgress(temp);
                        } else {
                            int pages = new Double(Math.ceil((float) patientCount / pageSize)).intValue();

                            for (int page = 1; page <= pages; page++) {
                                if (!isCancelled()) {
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
                                } else {
                                    break;
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
                Log.e(getClass().getSimpleName(), "Exception occurred while fetching patients", e);
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
            onPostExecuteUpdate(patients);
        }

        @Override
        protected void onBackgroundError(Exception e) {
            Log.e(getClass().getSimpleName(), "Error while running background task",e);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
