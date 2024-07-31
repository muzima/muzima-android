/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.adapters.forms.IncompleteFormsWithDataAdapter;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.FormWithData;
import com.muzima.utils.StringUtils;

import com.muzima.view.patients.PatientSummaryActivity;

public class IncompleteFormsListFragment extends FormsWithDataListFragment implements FormsAdapter.MuzimaClickListener{

    public static IncompleteFormsListFragment newInstance(FormController formController, ObservationController observationController) {
        IncompleteFormsListFragment f = new IncompleteFormsListFragment();
        f.formController = formController;
        f.observationController = observationController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String filterPatientUuid = getActivity().getIntent().getStringExtra(PatientSummaryActivity.PATIENT_UUID);
        listAdapter = new IncompleteFormsWithDataAdapter(getActivity(), R.layout.item_form_with_data_layout,filterPatientUuid,formController, observationController);
        ((IncompleteFormsWithDataAdapter)listAdapter).setMuzimaClickListener(this);
        noDataMsg = getActivity().getResources().getString(R.string.info_incomplete_form_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_incomplete_form_unavailable);

        if (actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((IncompleteFormsWithDataAdapter)listAdapter).getSelectedFormsUuids().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && isResumed()){
            logEvent("VIEW_INCOMPLETE_FORMS");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (FormWithData) listAdapter.getItem(position));
        getActivity().startActivityForResult(intent, FormsWithDataActivity.FORM_VIEW_ACTIVITY_RESULT);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    @Override
    public void onItemLongClick() {
        if (!actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = ((IncompleteFormsWithDataAdapter)listAdapter).getSelectedFormsUuids().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    public void onItemClick(int position, View view) {
        if(!actionModeActive) {
            FormWithData formWithData = (FormWithData) listAdapter.getItem(position);
            if (formWithData.getPatient() != null && StringUtils.isEmpty(formWithData.getPatient().getUuid())) {
                formWithData.setPatient(null);
            }
            FormViewIntent intent = new FormViewIntent(getActivity(), formWithData);
            getActivity().startActivityForResult(intent, FormsWithDataActivity.FORM_VIEW_ACTIVITY_RESULT);
        }
    }
}
