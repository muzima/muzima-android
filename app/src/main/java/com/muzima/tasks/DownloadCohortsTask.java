package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.List;

public class DownloadCohortsTask implements Runnable {
    private Context context;
    private List<Cohort> cohortList;
    private CohortDownloadCallback cohortDownloadCallback;
    public DownloadCohortsTask(Context context, List<Cohort> cohortList,CohortDownloadCallback cohortDownloadCallback) {
        this.context = context;
        this.cohortList = cohortList;
        this.cohortDownloadCallback = cohortDownloadCallback;
    }

    @Override
    public void run() {
        try {
            String[] cohortUuids = extractCohortUuids();
            ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .downloadCohortsByUuidList(cohortUuids);
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

    public interface CohortDownloadCallback{
        void callbackDownload();
    }
}
