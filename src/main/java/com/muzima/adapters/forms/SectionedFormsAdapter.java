/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */


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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersSectionIndexerAdapterWrapper;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.FormWithData;
import com.muzima.utils.PatientComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Responsible to list down the forms in the order of the Patient details. Here you can identify forms by the patient name.
 * @param <T> T is of the type FormsWithData.
 */
public abstract class SectionedFormsAdapter<T extends FormWithData> extends FormsAdapter<T> implements StickyListHeadersAdapter, SectionIndexer {
    private static final String TAG = "SectionedFormsAdapter";

    private List<Patient> patients;
    private ListView listView;
    private PatientComparator patientComparator;
    private List<String> selectedFormsUuid;
    private MuzimaClickListener muzimaClickListener;

    public SectionedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        patients = new ArrayList<Patient>();
        patientComparator = new PatientComparator();
        selectedFormsUuid = new ArrayList<String>();
    }

    @Override
    protected int getFormItemLayout() {
        return R.layout.item_forms_list_selectable;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.layout_forms_list_section_header, parent, false);
            holder.patientName = (TextView) convertView.findViewById(R.id.patientName);
            holder.patientIdentifier = (TextView) convertView.findViewById(R.id.patientId);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        if (!patients.isEmpty()) {
            Patient patient = patients.get(getSectionForPosition(position));
            if (patient != null) {
                holder.patientName.setText(patient.getDisplayName());
                holder.patientIdentifier.setText(patient.getIdentifier());
            } else {
                holder.patientName.setText("Registration Forms");
            }
        }
        return convertView;
    }

    public void setListView(ListView listView) {
        this.listView = listView;
    }

    @Override
    public long getHeaderId(int position) {
        int section = 0;
        if (!patients.isEmpty()) {
            section = patients.indexOf(getItem(position).getPatient());
        }
        return section;
    }

    @Override
    public Object[] getSections() {
        String[] familyNames = new String[patients.size()];
        for (int i = 0; i < patients.size(); i++) {
            familyNames[i] = String.valueOf(patients.get(i).getFamilyName().charAt(0));
        }
        return familyNames;
    }

    @Override
    public int getPositionForSection(int section) {
        if (section >= patients.size()) {
            section = patients.size() - 1;
        } else if (section < 0) {
            section = 0;
        }

        int position = 0;
        for (int i = 0; i < getCount(); i++) {
            FormWithData item = getItem(i);
            if (item.getPatient().equals(patients.get(section))) {
                position = i;
                break;
            }
        }
        return position;
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position >= getCount()) {
            position = getCount() - 1;
        } else if (position < 0) {
            position = 0;
        }

        int section = 0;
        if (!patients.isEmpty()){
            section = patients.indexOf(getItem(position).getPatient());
        }
        return section;
    }

    public void sortFormsByPatientName(List<T> forms) {
        Collections.sort(patients, patientComparator);
        Collections.sort(forms, alphabaticalComparator);
        setNotifyOnChange(false);
        clear();
        addAll(forms);

        StickyListHeadersSectionIndexerAdapterWrapper adapter = (StickyListHeadersSectionIndexerAdapterWrapper) listView.getAdapter();
        adapter.notifyDataSetChanged();
    }

    List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
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

                FormWithData formWithPatientData = getItem(position);
                if (!selectedFormsUuid.contains(formWithPatientData.getFormDataUuid())) {
                    selectedFormsUuid.add(formWithPatientData.getFormDataUuid());
                } else if (selectedFormsUuid.contains(formWithPatientData.getFormDataUuid())) {
                    selectedFormsUuid.remove(formWithPatientData.getFormDataUuid());
                }

                muzimaClickListener.onItemLongClick();
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


    private static class HeaderViewHolder {
        TextView patientName;
        TextView patientIdentifier;
    }

    private Comparator<FormWithData> alphabaticalComparator = new Comparator<FormWithData>() {
        @Override
        public int compare(FormWithData lhs, FormWithData rhs) {
            return patientComparator.compare(lhs.getPatient(), rhs.getPatient());
        }
    };

    protected List<Patient> buildPatientsList(List<T> forms) {
        List<Patient> result = new ArrayList<Patient>();
        for (FormWithData form : forms) {
            Patient patient = form.getPatient();
            if (!result.contains(patient)) {
                result.add(patient);
            }
        }
        return result;
    }
}
