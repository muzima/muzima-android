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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Tag;
import com.muzima.controller.PatientController;
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientAdapterHelper extends ListAdapter<Patient> {
    private PatientController patientController;

    public PatientAdapterHelper(Context context, int textViewResourceId, PatientController patientController) {
        super(context, textViewResourceId);
        this.patientController = patientController;
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
            holder.tagsScroller = convertView.findViewById(R.id.tags_scroller);
            holder.tagsLayout = convertView.findViewById(R.id.menu_tags);
            holder.tags = new ArrayList<>();
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        holder.dateOfBirth.setText(String.format("DOB: %s", getFormattedDate(patient.getBirthdate())));
        holder.identifier.setText(patient.getIdentifier());
        holder.name.setText(getPatientFullName(patient));
        holder.genderImg.setImageResource(getGenderImage(patient.getGender()));
        addTags(holder,patient);
        return convertView;
    }

    private void addTags(ViewHolder holder, Patient patient) {
        Tag[] tags = patient.getTags();
        if (tags.length > 0) {
            holder.tagsScroller.setVisibility(View.VISIBLE);
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            //add update tags
            for (int i = 0; i < tags.length; i++) {
                TextView textView = null;
                if (holder.tags.size() <= i) {
                    textView = newTextView(layoutInflater);
                    holder.addTag(textView);
                }
                textView = holder.tags.get(i);
                textView.setBackgroundColor(patientController.getTagColor(tags[i].getUuid()));
                List<Tag> selectedTags = patientController.getSelectedTags();
                if (selectedTags.isEmpty() || selectedTags.contains(tags[i])) {
                    textView.setText(tags[i].getName());
                } else {
                    textView.setText(StringUtils.EMPTY);
                }
            }

            //remove existing extra tags which are present because of recycled list view
            if (tags.length < holder.tags.size()) {
                List<TextView> tagsToRemove = new ArrayList<>();
                for (int i = tags.length; i < holder.tags.size(); i++) {
                    tagsToRemove.add(holder.tags.get(i));
                }
                holder.removeTags(tagsToRemove);
            }
        } else {
            holder.tagsScroller.setVisibility(View.INVISIBLE);
        }
    }

    private TextView newTextView(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
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
        List<TextView> tags;
        LinearLayout tagsLayout;
        RelativeLayout tagsScroller;

        public void addTag(TextView tag) {
            this.tags.add(tag);
            tagsLayout.addView(tag);
        }

        void removeTags(List<TextView> tagsToRemove) {
            for (TextView tag : tagsToRemove) {
                tagsLayout.removeView(tag);
            }
            tags.removeAll(tagsToRemove);
        }
    }
}