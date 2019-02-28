/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.adapters.forms.PatientCompleteFormsAdapter;
import com.muzima.adapters.forms.PatientIncompleteFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteForm;

public class CompletePatientsFormsListFragment extends FormsListFragment implements FormsAdapter.MuzimaClickListener{

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
        ((PatientCompleteFormsAdapter)listAdapter).setMuzimaClickListener(this);
        noDataMsg = getActivity().getResources().getString(R.string.info_complete_form_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_complete_form_unavailable);

        if (actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((PatientIncompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Store our shared preference
        SharedPreferences sp = getActivity().getSharedPreferences("COMPLETED_FORM_AREA_IN_FOREGROUND", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", true);
        ed.commit();
        listAdapter.reloadData();
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sp = getActivity().getSharedPreferences("COMPLETED_FORM_AREA_IN_FOREGROUND", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", false);
        ed.commit();
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
        int numOfSelectedForms = ((PatientCompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    public void onItemClick(int position) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (CompleteForm) listAdapter.getItem(position), patient);
        intent.putExtra(FormViewIntent.FORM_COMPLETION_STATUS_INTENT, FormViewIntent.FORM_COMPLETION_STATUS_COMPLETE);
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }

    public void onFormUploadFinish() {
        listAdapter.reloadData();
    }
}
