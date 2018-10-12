/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.view.forms.CompletePatientsFormsListFragment;
import com.muzima.view.forms.IncompletePatientsFormsListFragment;
import com.muzima.view.forms.RecommendedFormsListFragment;

import static com.muzima.utils.Constants.MuzimaGPSLocationConstants.LOCATION_ACCESS_PERMISSION_REQUEST_CODE;

/**
 * Responsible to hold all the form pages that are part of a specific patient.
 */
public class PatientFormsPagerAdapter extends MuzimaPagerAdapter {
    private static final int TAB_RECOMMENDED = 0;
    private static final int TAB_INCOMPLETE = 1;
    private static final int TAB_COMPLETE = 2;
    private final Patient patient;


    public PatientFormsPagerAdapter(Context context, FragmentManager fm, Patient patient) {
        super(context, fm);
        this.patient = patient;
    }

    @Override
    public void initPagerViews() {
        pagers = new PagerView[3];
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();

        IncompletePatientsFormsListFragment incompleteFormsListFragment = IncompletePatientsFormsListFragment.newInstance(formController, patient);
        RecommendedFormsListFragment recommendedFormsListFragment = RecommendedFormsListFragment.newInstance(formController, patient);
        CompletePatientsFormsListFragment completeFormsListFragment = CompletePatientsFormsListFragment.newInstance(formController, patient);

        pagers[TAB_INCOMPLETE] = new PagerView(context.getString(R.string.title_form_data_incomplete), incompleteFormsListFragment);
        pagers[TAB_RECOMMENDED] = new PagerView(context.getString(R.string.info_form_template_recommended), recommendedFormsListFragment);
        pagers[TAB_COMPLETE] = new PagerView(context.getString(R.string.title_form_data_complete), completeFormsListFragment);
    }

    public void onFormUploadFinish() {
        ((CompletePatientsFormsListFragment)pagers[TAB_COMPLETE].fragment).onFormUploadFinish();
    }


}
