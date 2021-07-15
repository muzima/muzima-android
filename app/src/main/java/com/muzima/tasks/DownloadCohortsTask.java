package com.muzima.tasks;

import android.content.Context;
import android.content.Intent;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.domain.Credentials;
import com.muzima.model.cohort.CohortItem;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.DataSyncService;
import com.muzima.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;

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
        Intent cohortMetadataIntent = new Intent(context.getApplicationContext(), DataSyncService.class);
        cohortMetadataIntent.putExtra(CREDENTIALS, new Credentials(context).getCredentialsArray());
        cohortMetadataIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_COHORTS_METADATA);
        context.startService(cohortMetadataIntent);
        Intent syncCohortDataIntent = new Intent(context.getApplicationContext(), DataSyncService.class);
        syncCohortDataIntent.putExtra(CREDENTIALS, new Credentials(context).getCredentialsArray());
        syncCohortDataIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_COHORTS_AND_ALL_PATIENTS_FULL_DATA);
        context.startService(syncCohortDataIntent);
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
