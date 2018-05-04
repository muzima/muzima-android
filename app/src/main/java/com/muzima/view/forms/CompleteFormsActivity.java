/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.adapters.forms.CompleteFormsAdapter;
import com.muzima.adapters.forms.PatientFormsPagerAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.api.service.FormService;

import com.muzima.model.CompleteForm;
import com.muzima.model.collections.CompleteForms;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.utils.Constants;
import com.muzima.view.patients.PatientSummaryActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.forms.CompleteFormsAdapter;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.controller.FormController.FormDataFetchException;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteFormWithPatientData;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CompleteFormsActivity extends FormsActivityBase {
    public static final String TAG = "FormsActivity";
    public Provider provider;
    public FormController formController;
    public FormService formService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        initPager();
        initPagerIndicator();

    }

    @Override
    protected MuzimaPagerAdapter createFormsPagerAdapter() {

        MuzimaPagerAdapter completeFormsAdapter = null;
        List<FormData> allCompleteForms = new ArrayList<FormData>();
        try {
            List<FormData> allFormData = formService.getAllFormData(Constants.STATUS_COMPLETE);
            if (allFormData.size() > 0) {
                for (FormData formData : allFormData) {
                    allCompleteForms.add(formData);
                }
            } else {
                Toast.makeText(getApplicationContext(), "No forms exist: ", Toast.LENGTH_LONG).show();
            }

        } catch (IOException e) {

        }


        return (MuzimaPagerAdapter) allCompleteForms;


    }
    public int getTotalFormCount() throws FormController.FormFetchException {       //Total forms
        try {
            return formService.countAllForms();
        } catch (IOException e) {
            throw new FormController.FormFetchException(e);
        }
    }
    public int getTotalTestedCount() throws FormController.FormFetchException {      //Total tested
        try {
            return formService.countFormByName("HTS Form");
        } catch (IOException e) {
            throw new FormController.FormFetchException(e);
        }
    }
    public int getTotalLinkedCount() throws FormController.FormFetchException {      //Total linked
        try {
            return formService.countFormByName("Referral and Linkage");
        } catch (IOException e) {
            throw new FormController.FormFetchException(e);
        }
    }
    public int getTotalContactsCount() throws FormController.FormFetchException {      //Total contacts
        try {
            return formService.countFormByName("Family History");
        } catch (IOException e) {
            throw new FormController.FormFetchException(e);
        }
    }
    public Form getTotalTestedPositiveCount() throws FormController.FormFetchException {      //Total tested Positive
        List<Form> allCompleteForms = new ArrayList<Form>();
        try {
            List<Form> htsFormData =  formService.getFormByName("HTS Form");
            if (htsFormData.size() > 0) {
                for (Form htsForm : htsFormData) {
                   // if(htsForm.getEncounterType().)
                    allCompleteForms.add(htsForm);
                }
            } else {
                Toast.makeText(getApplicationContext(), "No forms exist: ", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            throw new FormController.FormFetchException(e);
        }
        return null;
    }

}



