/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.muzima.adapters.forms;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.controller.FormController;
import com.muzima.model.FormWithData;
import com.muzima.view.CheckedRelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to list down the forms in the order of the Patient details. Here you can identify forms by the patient name.
 * @param <T> T is of the type FormsWithData.
 */
public abstract class FormsWithDataAdapter<T extends FormWithData> extends FormsAdapter<T> {
    private static final String TAG = "FormsWithDataAdapter";

    private List<String> selectedIncompleteFormsUuid;
    private MuzimaClickListener muzimaClickListener;


    public FormsWithDataAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        selectedIncompleteFormsUuid = new ArrayList<String>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);
        setClickListenersOnView(position, convertView);
        return convertView;
    }

    private void setClickListenersOnView(final int position, View convertView) {
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                CheckedRelativeLayout checkedLinearLayout = (CheckedRelativeLayout) view;
                checkedLinearLayout.toggle();
                boolean selected = checkedLinearLayout.isChecked();

                FormWithData incompleteFormWithPatientData = getItem(position);
                if (selected && !selectedIncompleteFormsUuid.contains(incompleteFormWithPatientData.getFormDataUuid())) {
                    selectedIncompleteFormsUuid.add(incompleteFormWithPatientData.getFormDataUuid());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        checkedLinearLayout.setChecked(true);
                    } else {
                        checkedLinearLayout.setActivated(true);

                    }
                } else if (!selected && selectedIncompleteFormsUuid.contains(incompleteFormWithPatientData.getFormDataUuid())) {
                    selectedIncompleteFormsUuid.remove(incompleteFormWithPatientData.getFormDataUuid());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        checkedLinearLayout.setChecked(false);
                    } else {
                        checkedLinearLayout.setActivated(false);
                    }
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

    public List<String> getSelectedIncompleteFormsUuid() {
        return selectedIncompleteFormsUuid;
    }

    public void clearSelectedIncompleteFormsUuid() {
        selectedIncompleteFormsUuid.clear();
    }

}
