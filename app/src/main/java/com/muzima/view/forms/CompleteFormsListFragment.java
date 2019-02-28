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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.CompleteFormsAdapter;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.utils.Constants;

public class CompleteFormsListFragment extends FormsFragmentWithSectionedListAdapter implements FormsAdapter.MuzimaClickListener{

    public static CompleteFormsListFragment newInstance(FormController formController) {
        CompleteFormsListFragment f = new CompleteFormsListFragment();
        f.formController = formController;
        return f;
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
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new CompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        ((CompleteFormsAdapter)listAdapter).setMuzimaClickListener(this);
        noDataMsg = getActivity().getResources().getString(R.string.info_complete_form_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_complete_form_unavailable);

        if (actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((CompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.layout_list_with_sections, container, false);
    }

    public void onFormUploadFinish() {
        listAdapter.reloadData();
    }

    @Override
    public void onItemLongClick() {
        if (!actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = ((CompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    public void onItemClick(int position) {
        CompleteFormWithPatientData completeFormWithPatientData = (CompleteFormWithPatientData) listAdapter.getItem(position);
        if(completeFormWithPatientData.getDiscriminator() != null) {
            if(!completeFormWithPatientData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS)
                    && !completeFormWithPatientData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION)) {
                FormViewIntent intent = new FormViewIntent(getActivity(), (CompleteFormWithPatientData) listAdapter.getItem(position));
                getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
            }
        }
    }
}
