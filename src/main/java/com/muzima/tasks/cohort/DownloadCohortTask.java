package com.muzima.tasks.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.utils.Constants;
import com.muzima.view.cohort.AllCohortsListFragment;
import com.muzima.view.forms.NewFormsListFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

        List<String> cohortPrefixes = getCohortPrefixes(values);

        try {
            List<Cohort> cohorts;
            if (cohortPrefixes == null) {
                cohorts = cohortController.downloadAllCohorts();
            } else {
                cohorts = cohortController.downloadCohortsByPrefix(cohortPrefixes);
            }
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

    private List<String> getCohortPrefixes(String[][] values) {
        if(values.length > 1){
            return Arrays.asList(values[1]);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer[] result) {
        SharedPreferences pref = applicationContext.getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Date date = new Date();
        editor.putLong(AllCohortsListFragment.COHORTS_LAST_SYNCED_TIME, date.getTime());
        editor.commit();
        super.onPostExecute(result);
    }
}
