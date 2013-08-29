package com.muzima.adapters.observations;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.CohortController;
import com.muzima.controller.PatientController;
import com.muzima.view.observations.ObservationByEncountersFragment;
import com.muzima.view.observations.ObservationsByDateListFragment;

public class ObservationsPagerAdapter extends MuzimaPagerAdapter{

    private static final int TAB_BY_DATE = 0;
    private static final int TAB_BY_ENCOUNTERS = 1;

    public ObservationsPagerAdapter(Context applicationContext, FragmentManager supportFragmentManager) {
        super(applicationContext,supportFragmentManager);
    }

    @Override
    protected void initPagerViews(Context context) {
        pagers = new PagerView[2];
        PatientController patientController = ((MuzimaApplication) context.getApplicationContext()).getPatientController();

        ObservationsByDateListFragment observationByDateListFragment = ObservationsByDateListFragment.newInstance(patientController);
        ObservationByEncountersFragment observationByEncountersFragment = ObservationByEncountersFragment.newInstance(patientController);

        pagers[TAB_BY_DATE] = new PagerView("By Date", observationByDateListFragment);
        pagers[TAB_BY_ENCOUNTERS] = new PagerView("By Encounters", observationByEncountersFragment);

    }
}
