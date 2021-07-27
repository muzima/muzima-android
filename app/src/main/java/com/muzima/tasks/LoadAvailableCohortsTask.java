package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoadAvailableCohortsTask implements Runnable {

    private Context context;
    private OnCohortsLoadedCallback cohortsLoadedCallback;

    public LoadAvailableCohortsTask(Context context, OnCohortsLoadedCallback onCohortsLoadedCallback) {
        this.context = context;
        this.cohortsLoadedCallback = onCohortsLoadedCallback;
    }

    @Override
    public void run() {
        try {
            List<Cohort> availableCohorts = new ArrayList<>();

            for (Cohort allCohort : ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getAllCohorts()) {
                if (!((MuzimaApplication) context.getApplicationContext()).getCohortController().isDownloaded(allCohort)) {
                    availableCohorts.add(allCohort);
                }
            }
            Collections.sort(availableCohorts, new Comparator<Cohort>() {
                @Override
                public int compare(Cohort o1, Cohort o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            cohortsLoadedCallback.onCohortsLoaded(availableCohorts);
        } catch (CohortController.CohortFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface OnCohortsLoadedCallback {
        void onCohortsLoaded(final List<Cohort> cohorts);
    }
}
