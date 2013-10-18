package com.muzima.adapters.forms;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.view.forms.CompletePatientsFormsListFragment;
import com.muzima.view.forms.IncompletePatientsFormsListFragment;
import com.muzima.view.forms.RecommendedFormsListFragment;

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
        PatientController patientController = ((MuzimaApplication) context.getApplicationContext()).getPatientController();

        IncompletePatientsFormsListFragment incompleteFormsListFragment = IncompletePatientsFormsListFragment.newInstance(formController, patient);
        RecommendedFormsListFragment recommendedFormsListFragment = RecommendedFormsListFragment.newInstance(formController, patient);
        CompletePatientsFormsListFragment completeFormsListFragment = CompletePatientsFormsListFragment.newInstance(formController, patient);

        pagers[TAB_INCOMPLETE] = new PagerView("Incomplete", incompleteFormsListFragment);
        pagers[TAB_RECOMMENDED] = new PagerView("Recommended", recommendedFormsListFragment);
        pagers[TAB_COMPLETE] = new PagerView("Complete", completeFormsListFragment);
    }
}
