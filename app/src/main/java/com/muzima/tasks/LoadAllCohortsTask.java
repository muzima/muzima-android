package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoadAllCohortsTask implements Runnable {

    private Context context;
    private OnAllCohortsLoadedCallback cohortsLoadedCallback;

    public LoadAllCohortsTask(Context context, OnAllCohortsLoadedCallback cohortsLoadedCallback) {
        this.context = context;
        this.cohortsLoadedCallback = cohortsLoadedCallback;
    }

    @Override
    public void run() {
        try {
            List<Cohort> cohorts = ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getAllCohorts();
            Collections.sort(cohorts, new Comparator<Cohort>() {
                @Override
                public int compare(Cohort o1, Cohort o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            cohortsLoadedCallback.onCohortsLoaded(cohorts);
        } catch (CohortController.CohortFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface OnAllCohortsLoadedCallback {
        void onCohortsLoaded(List<Cohort> cohorts);
    }
}
