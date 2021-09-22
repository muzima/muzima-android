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
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.model.AvailableForm;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;

public class EncounterMiniFormActivity extends AppCompatActivity {
    public static final String FORM_UUID = "formUuid";
    public static final String PATIENT_UUID = "patientUuid";

    private final ThemeUtils themeUtils = new ThemeUtils(true);
    private final LanguageUtil languageUtil = new LanguageUtil();
    private Patient patient;
    private AvailableForm form;
    private FormController formController;
    private PatientController patientController;
    private String formUuid;
    private String patientUuid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relationship_form_list);

        formUuid = getIntent().getStringExtra(FORM_UUID);
        patientUuid = getIntent().getStringExtra(PATIENT_UUID);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = (int) (displayMetrics.heightPixels*0.9);
        int width = (int) (displayMetrics.widthPixels*0.9);
        getWindow().setLayout(width, height);

        formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
        setFormAndPatient();
        startWebViewActivity(form,patient);
    }

    private void setFormAndPatient(){
        try {
             form = formController.getAvailableFormByFormUuid(formUuid);
             patient = patientController.getPatientByUuid(patientUuid);
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception while fetching forms");
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(),"Exception while fetching patients");
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
    }

    private void startWebViewActivity(AvailableForm form, Patient patient) {
        startActivity(new FormViewIntent(this, form, patient , true));
        finish();
    }
}
