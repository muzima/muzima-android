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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;
import com.muzima.utils.StringUtils;

import java.util.List;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientAdapterHelper extends ListAdapter<Patient> {

    public PatientAdapterHelper(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public View createPatientRow(Patient patient, View convertView, ViewGroup parent, Context context) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_patients_list_multi_checkable, parent, false);
            holder = new ViewHolder();
            holder.genderImg = convertView.findViewById(R.id.genderImg);
            holder.name = convertView.findViewById(R.id.name);
            holder.dateOfBirth = convertView.findViewById(R.id.dateOfBirth);
            holder.identifier = convertView.findViewById(R.id.identifier);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        holder.dateOfBirth.setText(String.format("DOB: %s", getFormattedDate(patient.getBirthdate())));
        holder.identifier.setText(patient.getIdentifier());
        holder.name.setText(getPatientFullName(patient));
        holder.genderImg.setImageResource(getGenderImage(patient.getGender()));
        return convertView;
    }

    public void onPreExecute(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskStarted();
        }
    }

    public void onPostExecute(List<Patient> patients, ListAdapter searchAdapter, BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (patients == null) {
            Toast.makeText(getContext(), getContext().getString(R.string.error_patient_repo_fetch), Toast.LENGTH_SHORT).show();
            return;
        }

        searchAdapter.clear();
        searchAdapter.addAll(patients);
        searchAdapter.notifyDataSetChanged();

        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }

    public void onProgressUpdate(List<Patient> patients, ListAdapter searchAdapter, BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        if (patients == null) {
            return;
        }

        searchAdapter.addAll(patients);
        searchAdapter.notifyDataSetChanged();

        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }

    public void onAuthenticationError(int searchResutStatus, BackgroundListQueryTaskListener backgroundListQueryTaskListener){
        backgroundListQueryTaskListener.onQueryTaskCancelled(searchResutStatus);
    }

    public void onNetworkError(SERVER_CONNECTIVITY_STATUS networkStatus, BackgroundListQueryTaskListener backgroundListQueryTaskListener){
        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskCancelled(networkStatus);
        }
    }

    public static String getPatientFormattedName(Patient patient) {
        StringBuilder patientFormattedName = new StringBuilder();
        if (!StringUtils.isEmpty(patient.getFamilyName())) {
            patientFormattedName.append(patient.getFamilyName());
            patientFormattedName.append(", ");
        }
        if (!StringUtils.isEmpty(patient.getGivenName())) {
            patientFormattedName.append(patient.getGivenName().substring(0, 1));
            patientFormattedName.append(" ");
        }
        if (!StringUtils.isEmpty(patient.getMiddleName())) {
            patientFormattedName.append(patient.getMiddleName().substring(0, 1));
        }
        return patientFormattedName.toString();
    }

    private String getPatientFullName(Patient patient) {
        StringBuilder patientFullName = new StringBuilder();
        if (!StringUtils.isEmpty(patient.getFamilyName())) {
            patientFullName.append(patient.getFamilyName());
            patientFullName.append(", ");
        }
        if (!StringUtils.isEmpty(patient.getGivenName())) {
            patientFullName.append(patient.getGivenName());
            patientFullName.append(" ");
        }
        if (!StringUtils.isEmpty(patient.getMiddleName())) {
            patientFullName.append(patient.getMiddleName());
        }
        return patientFullName.toString();
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
    }

    @Override
    public void reloadData() {
    }

    private class ViewHolder {
        ImageView genderImg;
        TextView name;
        TextView dateOfBirth;
        TextView identifier;
    }
}