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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.material.appbar.MaterialToolbar;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.RegistrationFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseActivity;
import com.muzima.view.BaseAuthenticatedActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.UUID;

import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;

public class RegistrationFormsActivity extends BaseAuthenticatedActivity {
    private RegistrationFormsAdapter registrationFormsAdapter;
    private Patient patient;
    private Patient indexPatient;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_form_list);

        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        indexPatient = (Patient) getIntent().getSerializableExtra(INDEX_PATIENT);


        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        ObservationController observationController = ((MuzimaApplication) getApplicationContext()).getObservationController();
        AvailableForms availableForms = getRegistrationForms(formController);
        if (availableForms.isEmpty()){
            showRegistrationFormsMissingAlert();
        }else {
            if (isOnlyOneRegistrationForm(availableForms)) {
                startWebViewActivity(availableForms.get(0));
            } else {
                prepareRegistrationAdapter(formController, availableForms, observationController);
            }
        }
        logEvent("VIEW_REGISTRATION_FORMS");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void showRegistrationFormsMissingAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationFormsActivity.this);
        builder.setCancelable(false)
                .setIcon(ThemeUtils.getIconWarning(getApplicationContext()))
                .setTitle(getResources().getString(R.string.general_alert))
                .setMessage(getResources().getString(R.string.general_registration_form_missing_message))
                .setPositiveButton(R.string.general_ok, launchDashboard())
                .show();
    }

    private DialogInterface.OnClickListener launchDashboard() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity( new Intent(getApplicationContext(), MainDashboardActivity.class));
                finish();
            }
        };
    }

    private void prepareRegistrationAdapter(FormController formController, AvailableForms availableForms, ObservationController observationController) {
        registrationFormsAdapter = new RegistrationFormsAdapter(this, R.layout.item_forms_list,
                formController, availableForms, observationController);
        ListView list = findViewById(R.id.list);
        list.setOnItemClickListener(startRegistrationOnClick());
        list.setAdapter(registrationFormsAdapter);
        registrationFormsAdapter.reloadData();
    }

    private boolean isOnlyOneRegistrationForm(AvailableForms availableForms) {
        return availableForms.size() == 1;
    }

    private AdapterView.OnItemClickListener startRegistrationOnClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AvailableForm form = registrationFormsAdapter.getItem(position);
                startWebViewActivity(form);
            }
        };
    }

    private void startWebViewActivity(AvailableForm form) {
        if (patient == null) {
            patient = new Patient();
            patient.setUuid(String.valueOf(UUID.randomUUID()));
        }
        Intent intent = new FormViewIntent(this, form, patient, false);
        intent.putExtra(INDEX_PATIENT, indexPatient);
        startActivity(intent);
        finish();
    }

    private AvailableForms getRegistrationForms(FormController formController) {
        AvailableForms availableForms = null;
        try {
            availableForms = formController.getDownloadedRegistrationForms();
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while retrieving registration forms from Lucene");
        }
        return availableForms;
    }
}
