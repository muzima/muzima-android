package com.muzima.tasks.cohort;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadMuzimaTask;

import java.util.List;

public class DownloadCohortTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadCohortTask";

    public DownloadCohortTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values){
        Integer[] result = new Integer[2];
        CohortController cohortController = applicationContext.getCohortController();

        try {
            List<Cohort> cohorts = cohortController.downloadAllCohorts();
            Log.i(TAG, "Cohort download successful, " + isCancelled());
            if (checkIfTaskIsCancelled(result)) return result;

            cohortController.deleteAllCohorts();
            Log.i(TAG, "Old cohorts are deleted");
            cohortController.saveAllCohorts(cohorts);
            Log.i(TAG, "New cohorts are saved");

            result[0] = SUCCESS;
            result[1] = cohorts.size();

        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception when trying to download cohorts");
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (CohortController.CohortSaveException e) {
            Log.e(TAG, "Exception when trying to save cohorts");
            result[0] = SAVE_ERROR;
            return result;
        } catch (CohortController.CohortDeleteException e) {
            Log.e(TAG, "Exception when trying to delete cohorts");
            result[0] = DELETE_ERROR;
            return result;
        }
        return result;
    }
}
