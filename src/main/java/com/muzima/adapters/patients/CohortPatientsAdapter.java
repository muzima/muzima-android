package com.muzima.adapters.patients;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Tag;
import com.muzima.controller.PatientController;

import java.util.List;

public class CohortPatientsAdapter extends ListAdapter<Patient> {
    private static final String TAG = "CohortPatientsAdapter";
    private PatientController patientController;
    private final String cohortId;

    public CohortPatientsAdapter(Context context, int textViewResourceId, PatientController patientController, String cohortId) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        this.cohortId = cohortId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_patients_list, parent, false);
            holder = new ViewHolder();
            holder.genderImg = (ImageView)convertView.findViewById(R.id.genderImg);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.dateOfBirth = (TextView) convertView.findViewById(R.id.dateOfBirth);
            holder.identifier = (TextView) convertView.findViewById(R.id.identifier);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        Patient patient = getItem(position);

        holder.dateOfBirth.setText(patient.getBirthdate().toString());
        holder.identifier.setText(patient.getIdentifier());
        holder.name.setText(String.format(patient.getFamilyName()+", "+patient.getGivenName()+", "+patient.getMiddleName()));
        int imgSrc = patient.getGender().equalsIgnoreCase("male")?R.drawable.ic_male:R.drawable.ic_female;
        holder.genderImg.setImageResource(imgSrc);
        return convertView;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(cohortId);
    }

    private class BackgroundQueryTask extends AsyncTask<String, Void, List<Patient>>{
        @Override
        protected List<Patient> doInBackground(String... cohortId) {
            List<Patient> patients  = null;
            try {
                patients = CohortPatientsAdapter.this.patientController.getPatients(cohortId[0]);
                Log.i(TAG, "#Patients in the cohort: " + patients.size());
            } catch (PatientController.LoadPatientException e) {
                Log.w(TAG, "Exception occurred while fetching patients" + e);
            }
            return patients;
        }

        @Override
        protected void onPostExecute(List<Patient> patients) {
            CohortPatientsAdapter.this.clear();

            for (Patient patient : patients) {
                add(patient);
            }
            notifyDataSetChanged();
        }
    }

    private class ViewHolder {
        ImageView genderImg;
        TextView name;
        TextView dateOfBirth;
        TextView identifier;
    }
}
