/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import androidx.annotation.NonNull;

import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.FormWithData;
import com.muzima.utils.PatientComparator;
import com.muzima.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.muzima.utils.Constants;

/**
 * Responsible to list down the forms in the order of the Patient details. Here you can identify forms by the patient name.
 * @param <T> T is of the type FormsWithData.
 */
public abstract class FormsWithDataAdapter<T extends FormWithData> extends FormsAdapter<T> {

    private String filterPatientUuid;
    private final PatientComparator patientComparator;
    private final List<String> selectedFormsUuids;
    private MuzimaClickListener muzimaClickListener;

    FormsWithDataAdapter(Context context, int textViewResourceId, String filterPatientUuid, FormController formController, ObservationController observationController) {
        super(context, textViewResourceId, formController,observationController);
        patientComparator = new PatientComparator();
        selectedFormsUuids = new ArrayList<>();
        this.filterPatientUuid = filterPatientUuid;
    }

    @Override
    protected int getFormItemLayout() {
        return R.layout.item_form_with_data_layout;
    }

    void sortFormsByPatientName(List<T> forms) {
        Collections.sort(forms, alphabaticalComparator);
        setNotifyOnChange(false);
        clear();
        addAll(forms);
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
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.STANDARD_DATE_FORMAT);

            if(StringUtils.isEmpty(filterPatientUuid)) {
                holder.formName.setText(formWithData.getName());
                if(formWithData.getPatient() != null) {
                    holder.clientName.setText(formWithData.getPatient().getDisplayName());
                    holder.clientId.setText(formWithData.getPatient().getIdentifier());
                }

                if (formWithData.getEncounterDate() != null) {
                    holder.encounterDate.setText(dateFormat.format(formWithData.getEncounterDate()));
                }
            } else {
                holder.clientName.setText(formWithData.getName());
                if (formWithData.getEncounterDate() != null) {
                    holder.formName.setText(dateFormat.format(formWithData.getEncounterDate()));
                }
            }

            if (formWithData.getLastModifiedDate() != null) {
                dateFormat = new SimpleDateFormat(Constants.STANDARD_DATE_LOCALE_FORMAT);
                String formSaveTime = dateFormat.format(formWithData.getLastModifiedDate());
                holder.lastEditedTime.setText(formSaveTime);
            }

            setClickListenersOnView(position, convertView);
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
                selectOrDeselectClickedItem(view,position);
                return true;
            }
        });
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(selectedFormsUuids.isEmpty()) {
                    setActivationBackgroundColor(view);
                    muzimaClickListener.onItemClick(position, view);
                } else {
                    selectOrDeselectClickedItem(view,position);
                }
            }
        });
    }

    private void selectOrDeselectClickedItem(View view, int position){
        FormWithData formWithPatientData = getItem(position);
        if ( !selectedFormsUuids.contains(formWithPatientData.getFormDataUuid())) {
            selectedFormsUuids.add(formWithPatientData.getFormDataUuid());
            view.setActivated(true);
            setActivationBackgroundColor(view);
        } else if ( selectedFormsUuids.contains(formWithPatientData.getFormDataUuid())) {
            selectedFormsUuids.remove(formWithPatientData.getFormDataUuid());
            view.setActivated(false);
        }
        muzimaClickListener.onItemLongClick();
    }

    private void setActivationBackgroundColor(View view){
        int[] attrs = new int[]{R.attr.activatedBackgroundIndicator};
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs);
        int backgroundResource = typedArray.getResourceId(0, 0);
        view.setBackgroundResource(backgroundResource);
    }

    public void setMuzimaClickListener(MuzimaClickListener muzimaClickListener) {
        this.muzimaClickListener = muzimaClickListener;
    }

    public List<String> getSelectedFormsUuids() {
        return selectedFormsUuids;
    }

    public void clearSelectedFormsUuid() {
        selectedFormsUuids.clear();
    }

    public void retainFromSelectedFormsUuid(Collection uuids) {
        selectedFormsUuids.retainAll(uuids);
    }

    private final Comparator<FormWithData> alphabaticalComparator = new Comparator<FormWithData>() {
        @Override
        public int compare(FormWithData lhs, FormWithData rhs) {
            return patientComparator.compare(lhs.getPatient(), rhs.getPatient());
        }
    };
}
