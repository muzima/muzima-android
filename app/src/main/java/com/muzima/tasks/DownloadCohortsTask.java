package com.muzima.tasks;

import android.content.Context;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.controller.CohortController;
import com.muzima.controller.PatientController;
import com.muzima.model.cohort.CohortItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadCohortsTask implements Runnable {
    private static final String TAG = "DownloadCohortsTask";
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
            List<CohortData> cohortDataList = ((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .downloadCohortData(cohortUuids, null);
            Log.e(TAG, "run: downloaded cohort data size " + cohortDataList.size());
            for (CohortData cohortData : cohortDataList) {
                ((MuzimaApplication) context.getApplicationContext()).getCohortController().saveAllCohorts(Collections.singletonList(cohortData.getCohort()));
                Log.e(TAG, "run: " + cohortData.getCohort().getName() + " members " + cohortData.getCohortMembers().size());
                for (CohortMember cohortMember : cohortData.getCohortMembers()) {
                    ((MuzimaApplication) context.getApplicationContext()).getPatientController().savePatient(cohortMember.getPatient());
                    Log.e(TAG, "run: cohort member " + cohortMember.getPatient().getDisplayName() + " gender " + cohortMember.getPatient().getGender());
                }
            }
            cohortDownloadCallback.callbackDownload();
        } catch (CohortController.CohortDownloadException | CohortController.CohortSaveException | PatientController.PatientSaveException ex) {
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
