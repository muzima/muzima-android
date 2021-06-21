package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.ArrayList;
import java.util.List;

public class CohortSearchTask implements Runnable {
    private Context context;
    private String searchTerm;
    private CohortSearchCallback callback;

    public CohortSearchTask(Context context, String searchTerm, CohortSearchCallback callback) {
        this.context = context;
        this.searchTerm = searchTerm;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            List<Cohort> searchResult = new ArrayList<>();
            for (Cohort allCohort : ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .getAllCohorts()) {
                if (allCohort.getName().startsWith(searchTerm))
                    searchResult.add(allCohort);
            }
            callback.onCohortSearchFinished(searchResult);
        } catch (CohortController.CohortFetchException ex) {
            ex.printStackTrace();
        }
    }

    public interface CohortSearchCallback {
        void onCohortSearchFinished(List<Cohort> cohortList);
    }
}
