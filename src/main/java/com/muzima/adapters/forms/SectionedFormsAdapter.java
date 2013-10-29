package com.muzima.adapters.forms;

import android.content.Context;
import android.os.Build;
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

public abstract class SectionedFormsAdapter<T extends FormWithData> extends FormsAdapter<T> implements StickyListHeadersAdapter, SectionIndexer {
    private static final String TAG = "SectionedFormsAdapter";

    private List<Patient> patients;
    private ListView listView;
    private PatientComparator patientComparator;

    public SectionedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        patients = new ArrayList<Patient>();
        patientComparator = new PatientComparator();
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

        Patient patient = patients.get(getSectionForPosition(position));
        holder.patientName.setText(patient.getDisplayName());
        holder.patientIdentifier.setText(patient.getIdentifier());

        return convertView;
    }

    public void setListView(ListView listView) {
        this.listView = listView;
    }

    @Override
    public long getHeaderId(int position) {
        return patients.indexOf(getItem(position).getPatient());
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

        return patients.indexOf(getItem(position).getPatient());
    }

    public void sortFormsByPatientName(List<T> forms) {
        Collections.sort(patients, patientComparator);
        Collections.sort(forms, alphabaticalComparator);
        setNotifyOnChange(false);
        clear();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            addAll(forms);
        } else {
//            TODO for FROYO
        }
        StickyListHeadersSectionIndexerAdapterWrapper adapter = (StickyListHeadersSectionIndexerAdapterWrapper) listView.getAdapter();
        adapter.notifyDataSetChanged();
    }

    List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
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
