package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.model.cohort.CohortItem;

import java.util.ArrayList;
import java.util.List;

public class DownloadCohortsTask implements Runnable {
    private Context context;
    private List<Cohort> cohortList = new ArrayList<>();
    private CohortDownloadCallback cohortDownloadCallback;

    public DownloadCohortsTask(Context context, List<Cohort> cohortList, CohortDownloadCallback cohortDownloadCallback) {
        this.context = context;
        this.cohortList = cohortList;
        this.cohortDownloadCallback = cohortDownloadCallback;
    }

    public DownloadCohortsTask(Context context, List<CohortItem> selectedCohorts, boolean holder, CohortDownloadCallback callback) {
        this.context = context;
        this.cohortDownloadCallback = callback;
        for (CohortItem selectedCohort : selectedCohorts) {
            if (selectedCohort.isSelected())
                this.cohortList.add(selectedCohort.getCohort());
        }
    }

    @Override
    public void run() {
        try {
            String[] cohortUuids = extractCohortUuids();
            ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .downloadCohortData(cohortUuids, null);
            cohortDownloadCallback.callbackDownload();
        } catch (CohortController.CohortDownloadException ex) {
            ex.printStackTrace();
        }
    }

    private String[] extractCohortUuids() {
        String[] cohortUuids = new String[cohortList.size()];
        for (int i = 0; i < cohortList.size(); i++) {
            cohortUuids[i] = cohortList.get(i).getUuid();
        }
        return cohortUuids;
    }

    public interface CohortDownloadCallback {
        void callbackDownload();
    }
}
