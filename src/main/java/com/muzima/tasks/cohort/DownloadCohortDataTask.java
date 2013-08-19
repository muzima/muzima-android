package com.muzima.tasks.cohort;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.FormTemplate;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadMuzimaTask;

import java.util.List;

import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;

public class DownloadCohortDataTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadCohortDataTask";

    public DownloadCohortDataTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values){
        Integer[] result = new Integer[2];
        CohortController cohortController = applicationContext.getCohortController();

        try{
            List<CohortData> cohortDataList = cohortController.downloadCohortData(values[1]);
            Log.i(TAG, "Cohort data download successful");

            if (checkIfTaskIsCancelled(result)) return result;

            for (CohortData cohortData : cohortDataList) {
                cohortController.replaceCohortMembers(cohortData.getUuid(), cohortData.getCohortMembers());
            }
            Log.i(TAG, "Form templates replaced");

            result[0] = SUCCESS;
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception thrown while downloading cohort data");
            result[0] =  DOWNLOAD_ERROR;
        } catch (CohortController.CohortReplaceException e) {
            Log.e(TAG, "Exception thrown while replacing cohort data");
            result[0] = REPLACE_ERROR;
        }
        return result;
    }
}
