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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.RegistrationFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.view.BaseActivity;

import java.util.UUID;

public class RegistrationFormsActivity extends BaseActivity {
    private RegistrationFormsAdapter registrationFormsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_form_list);

        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        AvailableForms availableForms = getRegistrationForms(formController);
        if (isOnlyOneRegistrationForm(availableForms)) {
            startWebViewActivity(availableForms.get(0));
        } else {
            prepareRegistrationAdapter(formController, availableForms);
        }
    }

    private void prepareRegistrationAdapter(FormController formController, AvailableForms availableForms) {
        registrationFormsAdapter = new RegistrationFormsAdapter(getApplicationContext(), R.layout.item_forms_list,
                formController, availableForms);
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
        Patient patient = new Patient();
        String uuid = String.valueOf(UUID.randomUUID());
        patient.setUuid(uuid);
        startActivity(new FormViewIntent(this, form, patient));
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
