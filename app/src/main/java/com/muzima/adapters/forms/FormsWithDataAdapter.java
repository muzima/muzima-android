/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import com.muzima.controller.FormController;
import com.muzima.model.FormWithData;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;
import com.muzima.view.CheckedRelativeLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;

/**
 * Responsible to list down the forms in the order of the Patient details. Here you can identify forms by the patient name.
 * @param <T> T is of the type FormsWithData.
 */
public abstract class FormsWithDataAdapter<T extends FormWithData> extends FormsAdapter<T> {

    private final List<String> selectedFormsUuid;
    private MuzimaClickListener muzimaClickListener;


    FormsWithDataAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        selectedFormsUuid = new ArrayList<>();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);
        setClickListenersOnView(position, convertView);
        FormWithData form = getItem(position);

        String formSaveTime = null;
        if(form.getLastModifiedDate() != null){
            SimpleDateFormat dateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT.concat(" HH:mm:ss"));
            formSaveTime = dateFormat.format(form.getLastModifiedDate());
        }

        String encounterDate = null;
        if(form.getEncounterDate() != null){
            SimpleDateFormat dateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT);
            encounterDate = dateFormat.format(form.getEncounterDate());
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();

        if (!StringUtils.isEmpty(formSaveTime)) {
            holder.savedTime.setText(formSaveTime);
        }
        if(!StringUtils.isEmpty(encounterDate)){
            holder.encounterDate.setText(encounterDate);
        }
        holder.savedTime.setTypeface(Fonts.roboto_italic(getContext()));
        holder.savedTime.setVisibility(View.VISIBLE);

        holder.encounterDate.setTypeface(Fonts.roboto_italic(getContext()));
        holder.savedTime.setVisibility(View.VISIBLE);

        return convertView;
    }

    private void setClickListenersOnView(final int position, View convertView) {
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
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
                return true;
            }

        });
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((CheckedRelativeLayout) view).toggle();
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

}
