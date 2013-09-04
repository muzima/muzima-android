package com.muzima.tasks.cohort;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.CohortController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.tasks.DownloadMuzimaTask;

import java.util.ArrayList;
import java.util.List;

public class DownloadCohortDataTask extends DownloadMuzimaTask {
    private static final String TAG = "DownloadCohortDataTask";

    public DownloadCohortDataTask(MuzimaApplication applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Integer[] performTask(String[]... values) {
        Integer[] result = new Integer[3];

        MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();

        if (muzimaApplicationContext == null) {
            result[0] = CANCELLED;
            return result;
        }

        CohortController cohortController = muzimaApplicationContext.getCohortController();
        PatientController patientController = muzimaApplicationContext.getPatientController();
        ObservationController observationController = muzimaApplicationContext.getObservationController();
        int patientCount = 0;
        try {
            long startDownloadCohortData = System.currentTimeMillis();
            String[] cohortUuids = values[1];
            List<CohortData> cohortDataList = cohortController.downloadCohortData(cohortUuids);
            long endDownloadCohortData = System.currentTimeMillis();
            Log.i(TAG, "Cohort data download successful with " + cohortDataList.size() + " cohorts");
            if (checkIfTaskIsCancelled(result)) return result;

            for (String cohortUuid : cohortUuids) {
                cohortController.deleteCohortMembers(cohortUuid);
            }

            for (CohortData cohortData : cohortDataList) {
                cohortController.addCohortMembers(cohortData.getCohortMembers());
                patientController.replacePatients(cohortData.getPatients());
                patientCount += cohortData.getPatients().size();
            }
            long cohortMemberAndPatientReplaceTime = System.currentTimeMillis();

            Log.i(TAG, "Cohort data replaced");
            Log.d(TAG, "Time Taken:\n " +
                    "In Downloading cohort data: " + (endDownloadCohortData - startDownloadCohortData) / 1000 + " sec\n" +
                    "In Replacing cohort members and patients: " + (cohortMemberAndPatientReplaceTime - endDownloadCohortData) / 1000 + " sec");
            Log.i(TAG, "Patients downloaded " + patientCount);
            Log.i(TAG, "Cohort data replaced");


            List<String> patientUuids = getPatientUuids(cohortDataList);

            long startDownloadObservations = System.currentTimeMillis();
            List<Observation> allObservations = getAllObservations(observationController, patientUuids);
            long endDownloadObservations = System.currentTimeMillis();
            Log.i(TAG, "Observations download successful with " + allObservations.size() + " observations");

            observationController.replaceObservations(patientUuids, allObservations);
            long replacedObservations = System.currentTimeMillis();

            Log.d(TAG, "In Downloading observations : " + (endDownloadObservations - startDownloadObservations) / 1000 + " sec\n" +
                    "In Replacing observations for patients: " + (replacedObservations - endDownloadObservations) / 1000 + " sec");

            result[0] = SUCCESS;
            result[1] = cohortDataList.size();
            result[2] = patientCount;
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception thrown while downloading cohort data" + e);
            result[0] = DOWNLOAD_ERROR;
        } catch (CohortController.CohortReplaceException e) {
            Log.e(TAG, "Exception thrown while replacing cohort data" + e);
            result[0] = REPLACE_ERROR;
        } catch (PatientController.PatientReplaceException e) {
            Log.e(TAG, "Exception thrown while replacing patients" + e);
            result[0] = REPLACE_ERROR;
        } catch (ObservationController.LoadObservationException e) {
            Log.e(TAG, "Exception thrown while replacing observations" + e);
            result[0] = REPLACE_ERROR;
        } catch (ObservationController.DownloadObservationException e) {
            Log.e(TAG, "Exception thrown while downloading observations" + e);
            result[0] = DOWNLOAD_ERROR;
        }
        return result;
    }

    private ArrayList<Observation> getAllObservations(ObservationController observationController, List<String> patientUuids) throws ObservationController.DownloadObservationException {
        ArrayList<Observation> allObservations = new ArrayList<Observation>();
        int index = 0;
        for (String patientUuid : patientUuids) {
            try {
                List<Observation> observations = observationController.downloadObservations(patientUuid);
                allObservations.addAll(observations);
            } catch (ObservationController.DownloadObservationException e) {
                Log.d(TAG, "Exception thrown while download observations for " + patientUuid);
            }
            index++;
            if (index % 5 == 0) {
                Log.i(TAG, index + "/" + patientUuids.size() + " patients' observations downloaded");
            }
        }
        return allObservations;
    }

    private List<String> getPatientUuids(List<CohortData> cohortDataList) {
        List<String> patientUuids = new ArrayList<String>();
        for (CohortData cohortData : cohortDataList) {
            for (Patient patient : cohortData.getPatients()) {
                patientUuids.add(patient.getUuid());
            }
        }
        return patientUuids;
    }
}
