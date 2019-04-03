/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SearchView;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.view.observations.ObservationByEncountersFragment;
import com.muzima.view.observations.ObservationsByConceptFragment;
import com.muzima.view.observations.ObservationsListFragment;

public class ObservationsPagerAdapter extends MuzimaPagerAdapter implements SearchView.OnQueryTextListener {

    private static final int TAB_BY_DATE = 0;
    private static final int TAB_BY_ENCOUNTERS = 1;
    private ObservationsListFragment observationByConceptListFragment;
    ObservationsListFragment observationByEncountersFragment;
    private Boolean isShrData;
    private Patient patient;

    public ObservationsPagerAdapter(Context applicationContext, FragmentManager supportFragmentManager, Boolean isShrData, Patient patient) {
        super(applicationContext, supportFragmentManager);
        this.isShrData = isShrData;
        this.patient = patient;
    }

    @Override
    public void initPagerViews() {
        pagers = new PagerView[2];
        ConceptController conceptController = ((MuzimaApplication) context.getApplicationContext()).getConceptController();
        ObservationController observationController = ((MuzimaApplication) context.getApplicationContext()).getObservationController();
        EncounterController encounterController = ((MuzimaApplication) context.getApplicationContext()).getEncounterController();

        observationByConceptListFragment =
                ObservationsByConceptFragment.newInstance(conceptController, observationController,isShrData,patient);
        observationByEncountersFragment = ObservationByEncountersFragment.newInstance(encounterController, observationController,isShrData);

        pagers[TAB_BY_DATE] = new PagerView(context.getString(R.string.title_observations_by_concepts), observationByConceptListFragment);
        pagers[TAB_BY_ENCOUNTERS] = new PagerView(context.getString(R.string.title_observations_by_encounters), observationByEncountersFragment);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return onQueryTextChange(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        for (PagerView pager : pagers) {
            ((ObservationsListFragment) pager.fragment).onSearchTextChange(newText);
        }
        return false;
    }

    public void cancelBackgroundQueryTasks() {
        observationByConceptListFragment.onQueryTaskCancelled();
        observationByEncountersFragment.onQueryTaskCancelled();
    }
}
