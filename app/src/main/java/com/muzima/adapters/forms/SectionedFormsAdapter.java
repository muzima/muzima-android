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
import android.support.annotation.NonNull;
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
import com.muzima.utils.Fonts;
import com.muzima.utils.PatientComparator;
import com.muzima.utils.StringUtils;
import com.muzima.view.CheckedRelativeLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;

/**
 * Responsible to list down the forms in the order of the Patient details. Here you can identify forms by the patient name.
 * @param <T> T is of the type FormsWithData.
 */
public abstract class SectionedFormsAdapter<T extends FormWithData> extends FormsAdapter<T> implements StickyListHeadersAdapter, SectionIndexer {

    private List<Patient> patients;
    private ListView listView;
    private final PatientComparator patientComparator;
    private final List<String> selectedFormsUuid;
    private MuzimaClickListener muzimaClickListener;

    SectionedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        patients = new ArrayList<>();
        patientComparator = new PatientComparator();
        selectedFormsUuid = new ArrayList<>();
    }

    @Override
    protected int getFormItemLayout() {
        return R.layout.item_forms_list_selectable;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if(!isEmpty()) {
            HeaderViewHolder holder;
            if (convertView == null) {
                holder = new HeaderViewHolder();
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                convertView = layoutInflater.inflate(R.layout.layout_forms_list_section_header, parent, false);
                holder.patientName = convertView.findViewById(R.id.patientName);
                holder.patientIdentifier = convertView.findViewById(R.id.patientId);
                setClickListenersOnView(position, convertView);
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
                    holder.patientName.setText(getContext().getString(R.string.default_form_register));
                }
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
        if (!patients.isEmpty() && !isEmpty()) {
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
            Patient patient = getItem(i).getPatient();
            if (patient != null && patient.equals(patients.get(section))) {
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
            Patient patient = getItem(position).getPatient();
            if (patient != null) {
                section = patients.indexOf(patient);
            }
        }
        return section;
    }

    void sortFormsByPatientName(List<T> forms) {
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

    void setPatients(List<Patient> patients) {
        this.patients = patients;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);
        if(!isEmpty()) {
            setClickListenersOnView(position, convertView);
            FormWithData form = getItem(position);

            String formSaveTime = null;
            if (form.getLastModifiedDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT.concat(" HH:mm:ss"));
                formSaveTime = dateFormat.format(form.getLastModifiedDate());
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();

            if (!StringUtils.isEmpty(formSaveTime)) {
                holder.savedTime.setText(formSaveTime);
            }
            holder.savedTime.setTypeface(Fonts.roboto_italic(getContext()));
            holder.savedTime.setVisibility(View.VISIBLE);
        }

        return convertView;
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


    private static class HeaderViewHolder {
        TextView patientName;
        TextView patientIdentifier;
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