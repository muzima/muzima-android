/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
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
            holder.genderImg = (ImageView) convertView.findViewById(R.id.genderImg);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.dateOfBirth = (TextView) convertView.findViewById(R.id.dateOfBirth);
            holder.identifier = (TextView) convertView.findViewById(R.id.identifier);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        holder.dateOfBirth.setText("DOB: " + getFormattedDate(patient.getBirthdate()));
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
            Toast.makeText(getContext(), "Something went wrong while fetching patients from local repo", Toast.LENGTH_SHORT).show();
            return;
        }

        searchAdapter.clear();
        searchAdapter.addAll(patients);
        searchAdapter.notifyDataSetChanged();

        if (backgroundListQueryTaskListener != null) {
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }

    public static String getPatientFormattedName(Patient patient){
        String familyName = patient.getFamilyName();
        String givenName = patient.getGivenName();
        String middleName = patient.getMiddleName();

        if(!givenName.equals("")){
            givenName =", "+ givenName.substring(0,1);
        }
        if(!middleName.equals("")){
            middleName = " "+middleName.substring(0,1);
        }
        return familyName + givenName + middleName;
    }

    private String getPatientFullName(Patient patient) {
        StringBuffer patientFullName = new StringBuffer();
        if(patient.getFamilyName()!=null) {
            patientFullName.append(patient.getFamilyName());
            patientFullName.append(", ");
        }
        if(patient.getGivenName() != null){
            patientFullName.append(patient.getGivenName());
            patientFullName.append(" ");
        }
        if(patient.getMiddleName() != null){
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
