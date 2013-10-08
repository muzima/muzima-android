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

    private PatientController patientController;
    private String patientId;

    public static CompletePatientsFormsListFragment newInstance(FormController formController, PatientController patientController, String patientId) {
        CompletePatientsFormsListFragment f = new CompletePatientsFormsListFragment();
        f.formController = formController;

        f.patientId = patientId;
        f.patientController = patientController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new PatientCompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController, patientId);

        Patient patient = null;
        String msgPostFix = " ";
        try {
            patient = patientController.getPatientByUuid(patientId);
            msgPostFix += patient.getGivenName() + " " + patient.getFamilyName();
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Exception thrown while fetching patient " + e);
            Toast.makeText(getActivity(), "An error occurred while fetching patient data", Toast.LENGTH_SHORT).show();
            msgPostFix += "Current Patient";
        }

        noDataMsg = getActivity().getResources().getString(R.string.no_complete_client_form_msg) + msgPostFix;
        noDataTip = getActivity().getResources().getString(R.string.no_incomplete_form_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (FormWithData) listAdapter.getItem(position));
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }
}
