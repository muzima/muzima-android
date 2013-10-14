package com.muzima.view.forms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.forms.PatientCompleteFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.model.FormWithData;

public class CompletePatientsFormsListFragment extends FormsListFragment {
    private static final String TAG = "CompletePatientsFormsListFragment";

    private Patient patient;

    public static CompletePatientsFormsListFragment newInstance(FormController formController, Patient patient) {
        CompletePatientsFormsListFragment f = new CompletePatientsFormsListFragment();
        f.formController = formController;

        f.patient = patient;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new PatientCompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController, patient.getUuid());

        noDataMsg = getActivity().getResources().getString(R.string.no_complete_client_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_incomplete_form_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (FormWithData) listAdapter.getItem(position), patient);
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }
}
