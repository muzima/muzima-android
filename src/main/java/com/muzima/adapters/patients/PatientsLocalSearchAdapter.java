package com.muzima.adapters.patients;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;

import java.util.List;

public class PatientsLocalSearchAdapter extends ListAdapter<Patient> {
    private static final String TAG = "PatientsLocalSearchAdapter";
    public static final String SEARCH = "search";
    private final PatientAdapterHelper patientAdapterHelper;
    private PatientController patientController;
    private final String cohortId;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public PatientsLocalSearchAdapter(Context context, int textViewResourceId,
                                      PatientController patientController, String cohortId) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        this.cohortId = cohortId;
        this.patientAdapterHelper = new PatientAdapterHelper(context,textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
       return patientAdapterHelper.createPatientRow(getItem(position),convertView,parent,getContext());
    }

    private String getPatientFullName(Patient patient) {
        return patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName();
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
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
            if (isSearch(params)) {
                try {
                    return patientController.searchPatientLocally(params[0], cohortId);
                } catch (PatientController.PatientLoadException e) {
                    Log.w(TAG, "Exception occurred while searching patients for " + params[0] + " search string. " + e);
                }
            }
            List<Patient> patients = null;
            String cohortUuid = params[0];
            try {
                if (cohortUuid != null) {
                    patients = patientController.getPatients(cohortUuid);
                } else {
                    patients = patientController.getAllPatients();
                }

            } catch (PatientController.PatientLoadException e) {
                Log.w(TAG, "Exception occurred while fetching patients" + e);
            }
            return patients;
        }

        private boolean isSearch(String[] params) {
            return params.length == 2 && SEARCH.equals(params[1]);
        }

        @Override
        protected void onPostExecute(List<Patient> patients) {
            patientAdapterHelper.onPostExecute(patients,PatientsLocalSearchAdapter.this,backgroundListQueryTaskListener);
        }
    }
    private class ViewHolder {
        ImageView genderImg;
        TextView name;
        TextView dateOfBirth;
        TextView identifier;
    }
}
