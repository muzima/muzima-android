package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.IncompleteFormsAdapter;
import com.muzima.adapters.forms.PatientIncompleteFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.IncompleteForm;

public class IncompletePatientsFormsListFragment extends FormsListFragment {

    private String patientId;

    public static IncompletePatientsFormsListFragment newInstance(FormController formController, String patientId) {
        IncompletePatientsFormsListFragment f = new IncompletePatientsFormsListFragment();
        f.formController = formController;
        f.patientId = patientId;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new PatientIncompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController, patientId);

        noDataMsg = getActivity().getResources().getString(R.string.no_incomplete_patient_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_incomplete_form_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (IncompleteForm) listAdapter.getItem(position));
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }
}
