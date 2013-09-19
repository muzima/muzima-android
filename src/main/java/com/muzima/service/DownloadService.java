package com.muzima.service;

import android.content.SharedPreferences;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.utils.Constants;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.*;

public class DownloadService {
    private static final String TAG = "DownloadService";

    private MuzimaApplication muzimaApplication;

    public DownloadService(MuzimaApplication muzimaContext) {
        this.muzimaApplication = muzimaContext;
    }

    public int authenticate(String[] credentials) {
        String username = credentials[0];
        String password = credentials[1];
        String server = credentials[2];

        Context muzimaContext = muzimaApplication.getMuzimaContext();
        try {
            muzimaContext.openSession();
            if (!muzimaContext.isAuthenticated()) {
                muzimaContext.authenticate(username, password, server);
            }
        } catch (ConnectException e) {
            return CONNECTION_ERROR;
        } catch (ParseException e) {
            return PARSING_ERROR;
        } catch (IOException e) {
            return AUTHENTICATION_ERROR;
        } finally {
            if (muzimaContext != null)
                muzimaContext.closeSession();
        }

        return AUTHENTICATION_SUCCESS;
    }

    public int[] downloadForms() {
        int[] result = new int[2];

        FormController formController = muzimaApplication.getFormController();

        try {
            List<Form> forms;
            forms = formController.downloadAllForms();
            Log.i(TAG, "Form download successful");
            formController.deleteAllForms();
            Log.i(TAG, "Old forms are deleted");
            formController.saveAllForms(forms);
            Log.i(TAG, "New forms are saved");

            result[0] = Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
            result[1] = forms.size();

        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (FormController.FormDeleteException e) {
            Log.e(TAG, "Exception occurred while deleting existing forms", e);
            result[0] = DELETE_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadFormTemplates(String[] formIds) {
        int[] result = new int[2];

        FormController formController = muzimaApplication.getFormController();

        try {
            List<FormTemplate> formTemplates = formController.downloadFormTemplates(formIds);
            Log.i(TAG, "Form template download successful");

            formController.replaceFormTemplates(formTemplates);
            Log.i(TAG, "Form templates replaced");

            result[0] = SUCCESS;
            result[1] = formTemplates.size();
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadCohorts() {
        int[] result = new int[2];
        CohortController cohortController = muzimaApplication.getCohortController();

        List<String> cohortPrefixes = getCohortPrefixes();
        try {
            List<Cohort> cohorts;
            if (cohortPrefixes.isEmpty())
                cohorts = cohortController.downloadAllCohorts();
            else
                cohorts = cohortController.downloadCohortsByPrefix(cohortPrefixes);
            Log.i(TAG, "Cohort download successful");
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


    private List<String> getCohortPrefixes() {
        SharedPreferences cohortSharedPref = muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE);
        Set<String> prefixes = cohortSharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>());
        return new ArrayList<String>(prefixes);
    }

}
