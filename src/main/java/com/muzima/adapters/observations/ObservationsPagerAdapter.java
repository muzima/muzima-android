package com.muzima.adapters.observations;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.widget.SearchView;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.ObservationController;
import com.muzima.view.observations.ObservationByEncountersFragment;
import com.muzima.view.observations.ObservationsByDateListFragment;
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
        ObservationController observationController = ((MuzimaApplication) context.getApplicationContext()).getObservationController();

        ObservationsListFragment observationByDateListFragment = ObservationsByDateListFragment.newInstance(observationController);
        ObservationsListFragment observationByEncountersFragment = ObservationByEncountersFragment.newInstance(observationController);

        pagers[TAB_BY_DATE] = new PagerView("By Date", observationByDateListFragment);
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
