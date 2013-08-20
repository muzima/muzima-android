package com.muzima.adapters.patients;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientsAdapter extends ListAdapter<Patient> {
    private static final String TAG = "PatientsAdapter";
    private PatientController patientController;
    private final String cohortId;

    public PatientsAdapter(Context context, int textViewResourceId, PatientController patientController, String cohortId) {
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
            holder.genderImg = (ImageView) convertView.findViewById(R.id.genderImg);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.dateOfBirth = (TextView) convertView.findViewById(R.id.dateOfBirth);
            holder.identifier = (TextView) convertView.findViewById(R.id.identifier);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        Patient patient = getItem(position);

        holder.dateOfBirth.setText("DOB: " + getFormattedDate(patient.getBirthdate()));
        holder.identifier.setText(patient.getIdentifier());
        holder.name.setText(String.format(patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName()));
        holder.genderImg.setImageResource(getGenderImage(patient.getGender()));
        return convertView;
    }

    private int getGenderImage(String gender) {
        int imgSrc = gender.equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
        return imgSrc;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(cohortId);
    }

    private class BackgroundQueryTask extends AsyncTask<String, Void, List<Patient>> {
        @Override
        protected List<Patient> doInBackground(String... cohortId) {
            List<Patient> patients = null;
            String cohortUuid = cohortId[0];
            try {
                if (cohortUuid != null) {
                    patients = patientController.getPatients(cohortUuid);
                    Log.i(TAG, "#Patients in the cohort " + cohortUuid + ": " + patients.size());
                } else {
                    patients = patientController.getAllPatients();
                    Log.i(TAG, "#All patients: " + patients.size());
                }

            } catch (PatientController.PatientLoadException e) {
                Log.w(TAG, "Exception occurred while fetching patients" + e);
            }
            return patients;
        }

        @Override
        protected void onPostExecute(List<Patient> patients) {
            PatientsAdapter.this.clear();

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
