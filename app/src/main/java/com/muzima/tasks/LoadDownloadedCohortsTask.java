package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoadDownloadedCohortsTask implements Runnable {

    private Context context;
    private OnDownloadedCohortsLoadedCallback cohortsLoadedCallback;

    public LoadDownloadedCohortsTask(Context context, OnDownloadedCohortsLoadedCallback cohortsLoadedCallback) {
        this.context = context;
        this.cohortsLoadedCallback = cohortsLoadedCallback;
    }

    @Override
    public void run() {
        try {
            List<Cohort> downloadedCohorts = new ArrayList<>();
            for (Cohort allCohort : ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getAllCohorts()) {
                if (((MuzimaApplication) context.getApplicationContext()).getCohortController().isDownloaded(allCohort)) {
                    downloadedCohorts.add(allCohort);
                }
            }
            Collections.sort(downloadedCohorts, new Comparator<Cohort>() {
                @Override
                public int compare(Cohort o1, Cohort o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            cohortsLoadedCallback.onCohortsLoaded(downloadedCohorts);
        } catch (CohortController.CohortFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface OnDownloadedCohortsLoadedCallback {
        void onCohortsLoaded(List<Cohort> cohorts);
    }
}
