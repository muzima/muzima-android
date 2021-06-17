package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

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
            cohortsLoadedCallback.onCohortsLoaded(((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getAllCohorts());
        } catch (CohortController.CohortFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface OnAllCohortsLoadedCallback {
        void onCohortsLoaded(List<Cohort> cohorts);
    }
}
