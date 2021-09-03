/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.FormWithData;
import com.muzima.utils.PatientComparator;
import com.muzima.view.custom.CheckedRelativeLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;
import static com.muzima.utils.Constants.STANDARD_DATE_LOCALE_FORMAT;

/**
 * Responsible to list down the forms in the order of the Patient details. Here you can identify forms by the patient name.
 * @param <T> T is of the type FormsWithData.
 */
public abstract class FormsWithDataAdapter<T extends FormWithData> extends FormsAdapter<T> {

    private List<Patient> patients;
    private final PatientComparator patientComparator;
    private final List<String> selectedFormsUuid;
    private MuzimaClickListener muzimaClickListener;

    FormsWithDataAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        patients = new ArrayList<>();
        patientComparator = new PatientComparator();
        selectedFormsUuid = new ArrayList<>();
    }

    @Override
    protected int getFormItemLayout() {
        return R.layout.item_form_with_data_layout;
    }

    public long getHeaderId(int position) {
        int section = 0;
        if (!patients.isEmpty() && !isEmpty()) {
            section = patients.indexOf(getItem(position).getPatient());
        }
        return section;
    }

    void sortFormsByPatientName(List<T> forms) {
        Collections.sort(patients, patientComparator);
        Collections.sort(forms, alphabaticalComparator);
        setNotifyOnChange(false);
        clear();
        addAll(forms);
    }

    List<Patient> getPatients() {
        return patients;
    }

    void setPatients(List<Patient> patients) {
        this.patients = patients;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FormWithDataViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    getFormItemLayout(), parent, false);
            holder = new FormWithDataViewHolder();
            holder.formName = convertView.findViewById(R.id.form_name);
            holder.clientName = convertView.findViewById(R.id.client_name);
            holder.clientId = convertView.findViewById(R.id.client_id);
            holder.lastEditedTime = convertView.findViewById(R.id.last_edited_date);
            holder.encounterDate = convertView.findViewById(R.id.encounter_date);
            holder.tagsScroller = convertView.findViewById(R.id.tags_scroller);
            holder.tagsLayout = convertView.findViewById(R.id.menu_tags);
            holder.tags = new ArrayList<>();

            convertView.setTag(holder);
        } else {
            holder = (FormWithDataViewHolder) convertView.getTag();
        }

        if (!isEmpty()) {
            FormWithData formWithData = getItem(position);

            System.out.println(formWithData.getPatient().getDisplayName());
            System.out.println(("IS NULLLLLLLLLLLLLLLLLL: "+(holder.clientName == null)));
            holder.formName.setText(formWithData.getName());
            holder.clientName.setText(formWithData.getPatient().getDisplayName());
            holder.clientId.setText(formWithData.getPatient().getIdentifier());

            SimpleDateFormat dateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT);

            if(formWithData.getEncounterDate() != null) {
                holder.encounterDate.setText(dateFormat.format(formWithData.getEncounterDate()));
            }

            //holder.lastEditedTime.setVisibility(View.GONE);

            setClickListenersOnView(position, convertView);

            if (formWithData.getLastModifiedDate() != null) {
                dateFormat = new SimpleDateFormat(STANDARD_DATE_LOCALE_FORMAT);
                String formSaveTime = dateFormat.format(formWithData.getLastModifiedDate());
                holder.lastEditedTime.setText(formSaveTime);
            }
            //holder.lastEditedTime.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    static class FormWithDataViewHolder {
        TextView formName;
        RelativeLayout tagsScroller;
        LinearLayout tagsLayout;
        List<TextView> tags;
        TextView clientName;
        TextView clientId;
        TextView lastEditedTime;
        TextView encounterDate;

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

    private void setClickListenersOnView(final int position, View convertView) {
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (view instanceof CheckedRelativeLayout) {
                    CheckedRelativeLayout checkedLinearLayout = (CheckedRelativeLayout) view;
                    checkedLinearLayout.toggle();
                    boolean selected = checkedLinearLayout.isChecked();

                    FormWithData formWithPatientData = getItem(position);
                    if (selected && !selectedFormsUuid.contains(formWithPatientData.getFormDataUuid())) {
                        selectedFormsUuid.add(formWithPatientData.getFormDataUuid());
                        checkedLinearLayout.setActivated(true);
                    } else if (!selected && selectedFormsUuid.contains(formWithPatientData.getFormDataUuid())) {
                        selectedFormsUuid.remove(formWithPatientData.getFormDataUuid());
                        checkedLinearLayout.setActivated(false);
                    }
                    muzimaClickListener.onItemLongClick();
                }
                return true;
            }
        });
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                muzimaClickListener.onItemClick(position);
            }
        });
    }

    public void setMuzimaClickListener(MuzimaClickListener muzimaClickListener) {
        this.muzimaClickListener = muzimaClickListener;
    }

    public List<String> getSelectedFormsUuid() {
        return selectedFormsUuid;
    }

    public void clearSelectedFormsUuid() {
        selectedFormsUuid.clear();
    }

    public void retainFromSelectedFormsUuid(Collection uuids) {
        selectedFormsUuid.retainAll(uuids);
    }

    private final Comparator<FormWithData> alphabaticalComparator = new Comparator<FormWithData>() {
        @Override
        public int compare(FormWithData lhs, FormWithData rhs) {
            return patientComparator.compare(lhs.getPatient(), rhs.getPatient());
        }
    };

    List<Patient> buildPatientsList(List<T> forms) {
        List<Patient> result = new ArrayList<>();
        for (FormWithData form : forms) {
            Patient patient = form.getPatient();
            if (patient != null && !result.contains(patient)) {
                result.add(patient);
            }
        }
        return result;
    }
}
