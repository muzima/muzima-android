package com.muzima.tasks;

import android.content.Context;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.model.cohort.CohortItem;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.MuzimaSyncService;

import java.util.ArrayList;
import java.util.List;

public class DownloadCohortsTask implements Runnable {
    private Context context;
    private List<Cohort> cohortList = new ArrayList<>();
    private CohortDownloadCallback cohortDownloadCallback;

    public DownloadCohortsTask(Context context, List<CohortItem> selectedCohorts, CohortDownloadCallback callback) {
        this.context = context;
        this.cohortDownloadCallback = callback;
        for (CohortItem selectedCohort : selectedCohorts) {
            if (selectedCohort.isSelected())
                this.cohortList.add(selectedCohort.getCohort());
        }
    }

    @Override
    public void run() {
        String[] cohortUuids = extractCohortUuids();
        String[] cohortPrefixes = extractCohortPrefixes();
        CohortPrefixPreferenceService preferenceService = new CohortPrefixPreferenceService(context);
        for (String cohortPrefix : cohortPrefixes) {
            preferenceService.addCohortPrefix(cohortPrefix);
        }
        ((MuzimaApplication) context.getApplicationContext()).getMuzimaSyncService().downloadCohorts(cohortUuids);
        new MuzimaSyncService(((MuzimaApplication) context.getApplicationContext())).downloadPatientsForCohorts(cohortUuids);
        cohortDownloadCallback.callbackDownload();
    }

    private String[] extractCohortUuids() {
        String[] cohortUuids = new String[cohortList.size()];
        for (int i = 0; i < cohortList.size(); i++) {
            cohortUuids[i] = cohortList.get(i).getUuid();
        }
        return cohortUuids;
    }

    private String[] extractCohortPrefixes() {
        String[] prefixes = new String[cohortList.size()];
        for (int i = 0; i < cohortList.size(); i++) {
            prefixes[i] = cohortList.get(i).getName();
        }
        return prefixes;
    }

    public interface CohortDownloadCallback {
        void callbackDownload();
    }
}
