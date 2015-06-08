/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import com.actionbarsherlock.widget.SearchView;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.view.observations.ObservationByEncountersFragment;
import com.muzima.view.observations.ObservationsByConceptFragment;
import com.muzima.view.patients.ObservationsListFragment;

public class ObservationsPagerAdapter extends MuzimaPagerAdapter implements SearchView.OnQueryTextListener {

    private static final int TAB_BY_DATE = 0;
    private static final int TAB_BY_ENCOUNTERS = 1;

    public ObservationsPagerAdapter(Context applicationContext, FragmentManager supportFragmentManager) {
        super(applicationContext,supportFragmentManager);
    }

    @Override
    public void initPagerViews() {
        pagers = new PagerView[2];
        ConceptController conceptController = ((MuzimaApplication) context.getApplicationContext()).getConceptController();
        ObservationController observationController = ((MuzimaApplication) context.getApplicationContext()).getObservationController();

        ObservationsListFragment observationByDateListFragment =
                ObservationsByConceptFragment.newInstance(conceptController, observationController);
        ObservationsListFragment observationByEncountersFragment = ObservationByEncountersFragment.newInstance(observationController);

        pagers[TAB_BY_DATE] = new PagerView("By Concepts", observationByDateListFragment);
        pagers[TAB_BY_ENCOUNTERS] = new PagerView("By Encounters", observationByEncountersFragment);

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return onQueryTextChange(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        for (PagerView pager : pagers) {
            ((ObservationsListFragment)pager.fragment).onSearchTextChange(newText);
        }
        return false;
    }
}
