package com.muzima.view.forms;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.adapters.forms.PatientIncompleteFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.IncompleteForm;

public class IncompletePatientsFormsListFragment extends FormsListFragment implements FormsAdapter.MuzimaClickListener {

    private Patient patient;

    public static IncompletePatientsFormsListFragment newInstance(FormController formController, Patient patient) {
        IncompletePatientsFormsListFragment f = new IncompletePatientsFormsListFragment();
        f.formController = formController;
        f.patient = patient;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new PatientIncompleteFormsAdapter(getActivity(), R.layout.item_forms_list_selectable, formController, patient.getUuid());
        ((PatientIncompleteFormsAdapter)listAdapter).setMuzimaClickListener(this);
        noDataMsg = getActivity().getResources().getString(R.string.no_incomplete_client_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_incomplete_form_tip);

        if (actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((PatientIncompleteFormsAdapter)listAdapter).getSelectedIncompleteFormsUuid().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    }

    @Override
    public boolean onItemLongClick() {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = ((PatientIncompleteFormsAdapter)listAdapter).getSelectedIncompleteFormsUuid().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
        return false;
    }

    @Override
    public void onItemClick(int position) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (IncompleteForm) listAdapter.getItem(position), patient);
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }
}
