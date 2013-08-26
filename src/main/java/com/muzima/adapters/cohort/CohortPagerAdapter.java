package com.muzima.adapters.cohort;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import android.view.View;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.CohortController;
import com.muzima.listeners.DownloadListener;
import com.muzima.view.cohort.AllCohortsListFragment;
import com.muzima.view.cohort.SyncedCohortsListFragment;

public class CohortPagerAdapter extends MuzimaPagerAdapter implements DownloadListener<Integer[]> {
    private static final String TAG = "CohortPagerAdapter";

    private static final int TAB_SYNCED = 0;
    private static final int TAB_All = 1;

    public CohortPagerAdapter(Context context, FragmentManager supportFragmentManager) {
        super(context, supportFragmentManager);
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        pagers[TAB_All].fragment.synchronizationComplete(result);
    }

    @Override
    public void downloadTaskStart() {

    }

    protected void initPagerViews(Context context){
        pagers = new PagerView[2];
        CohortController cohortController = ((MuzimaApplication) context.getApplicationContext()).getCohortController();

        AllCohortsListFragment allCohortsListFragment = AllCohortsListFragment.newInstance(cohortController);
        SyncedCohortsListFragment syncedCohortsListFragment = SyncedCohortsListFragment.newInstance(cohortController);

        allCohortsListFragment.setCohortDataDownloadListener(syncedCohortsListFragment);

        pagers[TAB_SYNCED] = new PagerView("Synced", syncedCohortsListFragment);
        pagers[TAB_All] = new PagerView("All", allCohortsListFragment);
    }
}
