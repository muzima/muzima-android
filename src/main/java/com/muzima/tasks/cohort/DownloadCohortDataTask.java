package com.muzima.tasks.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.CohortData;
import com.muzima.controller.CohortController;
import com.muzima.controller.PatientController;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.utils.Constants;
import com.muzima.view.forms.NewFormsListFragment;

import java.util.Date;
import java.util.List;

public class DownloadCohortDataTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadCohortDataTask";

    public DownloadCohortDataTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values){
        Integer[] result = new Integer[3];
        CohortController cohortController = applicationContext.getCohortController();
        PatientController patientController = applicationContext.getPatientController();
        int patientCount = 0;
        try{
            List<CohortData> cohortDataList = cohortController.downloadCohortData(values[1]);
            Log.i(TAG, "Cohort data download successful with " + cohortDataList.size() + " cohorts");

            if (checkIfTaskIsCancelled(result)) return result;

            for (CohortData cohortData : cohortDataList) {
                cohortController.replaceCohortMembers(cohortData.getUuid(), cohortData.getCohortMembers());
                patientController.replacePatients(cohortData.getPatients());
                patientCount += cohortData.getPatients().size();
            }

            Log.i(TAG, "Cohort data replaced");
            Log.i(TAG, "Patients downloaded " + patientCount);

            result[0] = SUCCESS;
            result[1] = cohortDataList.size();
            result[2] = patientCount;
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception thrown while downloading cohort data");
            result[0] =  DOWNLOAD_ERROR;
        } catch (CohortController.CohortReplaceException e) {
            Log.e(TAG, "Exception thrown while replacing cohort data");
            result[0] = REPLACE_ERROR;
        } catch (PatientController.PatientReplaceException e) {
            Log.e(TAG, "Exception thrown while replacing patients");
            result[0] = REPLACE_ERROR;
        }
        return result;
    }
}
