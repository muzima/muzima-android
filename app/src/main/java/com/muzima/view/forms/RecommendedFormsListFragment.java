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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.forms.RecommendedFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.BaseForm;
import com.muzima.view.patients.PatientsListActivity;

public class RecommendedFormsListFragment extends FormsListFragment implements AllAvailableFormsListFragment.OnTemplateDownloadComplete {
    private Patient patient;

    public static RecommendedFormsListFragment newInstance(FormController formController, Patient patient) {
        RecommendedFormsListFragment f = new RecommendedFormsListFragment();
        f.formController = formController;
        f.patient = patient;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new RecommendedFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        noDataMsg = getActivity().getResources().getString(R.string.info_downloaded_forms_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_recommended_forms_unavailable);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (AvailableForm) listAdapter.getItem(position), patient);
        intent.putExtra(FormViewIntent.FORM_COMPLETION_STATUS_INTENT,FormViewIntent.FORM_COMPLETION_STATUS_RECOMMENDED);
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }

    @Override
    public void onTemplateDownloadComplete() {
        listAdapter.reloadData();
    }

    @Override
    public void onResume() {
        Activity activity = getActivity();
        if (activity == null) {
            startActivity(new Intent(getActivity(),PatientsListActivity.class));
        }
        super.onResume();
    }
}
