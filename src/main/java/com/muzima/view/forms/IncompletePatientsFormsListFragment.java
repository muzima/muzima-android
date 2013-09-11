package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.PatientIncompleteFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.IncompleteForm;

public class IncompletePatientsFormsListFragment extends FormsListFragment {

    private String patientId;

    public static IncompletePatientsFormsListFragment newInstance(FormController formController, String patientId) {
        IncompletePatientsFormsListFragment f = new IncompletePatientsFormsListFragment();
        f.formController = formController;

        f.patientId = patientId;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new PatientIncompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController, patientId);

        noDataMsg = getActivity().getResources().getString(R.string.no_incomplete_patient_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_incomplete_form_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        startActivity(new FormViewIntent(getActivity(), (IncompleteForm) listAdapter.getItem(position)));
    }
}
