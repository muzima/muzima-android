package com.muzima.tasks.cohort;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.CohortData;
import com.muzima.controller.CohortController;
import com.muzima.controller.PatientController;
import com.muzima.tasks.DownloadMuzimaTask;

import java.util.List;

public class DownloadCohortDataTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadCohortDataTask";

    public DownloadCohortDataTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values){
        Integer[] result = new Integer[3];

        MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();

        if (muzimaApplicationContext == null) {
            result[0] = CANCELLED;
            return result;
        }

        CohortController cohortController = muzimaApplicationContext.getCohortController();
        PatientController patientController = muzimaApplicationContext.getPatientController();
        int patientCount = 0;
        try{
            long i = System.currentTimeMillis();
            List<CohortData> cohortDataList = cohortController.downloadCohortData(values[1]);
            long downloadTime = System.currentTimeMillis();
            Log.i(TAG, "Cohort data download successful with " + cohortDataList.size() + " cohorts");

            if (checkIfTaskIsCancelled(result)) return result;

            for (CohortData cohortData : cohortDataList) {
                cohortController.replaceCohortMembers(cohortData.getUuid(), cohortData.getCohortMembers());
                patientController.replacePatients(cohortData.getPatients());
                patientCount += cohortData.getPatients().size();
            }
            long cohortMemberAndPatientReplaceTime = System.currentTimeMillis();

            Log.i(TAG, "Cohort data replaced");
            Log.d(TAG, "Time Taken:\n " +
                    "In Downloading data: " + (downloadTime - i)/1000 + " sec\n" +
                    "In Replacing cohort members and patients: " + (cohortMemberAndPatientReplaceTime - downloadTime)/1000 + " sec");
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
