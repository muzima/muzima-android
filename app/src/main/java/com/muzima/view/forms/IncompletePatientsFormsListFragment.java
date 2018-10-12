/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

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
        noDataMsg = getActivity().getResources().getString(R.string.info_incomplete_form_unvailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_incomplete_form_unavailable);

        if (actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((PatientIncompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    }

    @Override
    public void onItemLongClick() {
        if (!actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = ((PatientIncompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    public void onItemClick(int position) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (IncompleteForm) listAdapter.getItem(position), patient);
        intent.putExtra(FormViewIntent.FORM_COMPLETION_STATUS_INTENT, FormViewIntent.FORM_COMPLETION_STATUS_INCOMPLETE);
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }
}
