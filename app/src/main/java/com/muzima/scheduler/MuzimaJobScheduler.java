package com.muzima.scheduler;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
import com.muzima.api.model.Person;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.ConceptIcons;
import com.muzima.model.DownloadedForm;
import com.muzima.model.IncompleteFormWithPatientData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.model.collections.DownloadedForms;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.util.JsonUtils;
import com.muzima.utils.ProcessedTemporaryFormDataCleanUpIntent;
import com.muzima.utils.SyncCohortsAndPatientFullDataIntent;
import com.muzima.utils.SyncSettingsIntent;
import com.muzima.view.forms.SyncFormIntent;
import com.muzima.view.forms.SyncFormTemplateIntent;
import com.muzima.view.patients.SyncPatientDataIntent;
import com.muzima.view.reports.SyncAllPatientReports;
import com.muzima.view.reports.SyncReportDatasets;
import com.muzima.view.setupconfiguration.SyncSetupConfigurationTemplates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressLint("NewApi")
public class MuzimaJobScheduler extends JobService {

    private MuzimaSyncService muzimaSynService;
    private String authenticatedUserUuid;
    private User authenticatedUser;
    private Person person;
    private boolean isAuthPerson = false;
    private MuzimaSettingController muzimaSettingController;

    @Override
    public void onCreate() {
        super.onCreate();
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        muzimaSettingController = muzimaApplication.getMuzimaSettingController();
        muzimaSynService = muzimaApplication.getMuzimaSyncService();
        authenticatedUser = muzimaApplication.getAuthenticatedUser();
        if (authenticatedUser != null){
            person = authenticatedUser.getPerson();

            if (person != null){
                authenticatedUserUuid = person.getUuid();
                isAuthPerson = true;
            }else{
                isAuthPerson = false;
            }

        }else {
            isAuthPerson = false;
            Log.i(getClass().getSimpleName(), "Authenticated user is not a person");
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {

        if (authenticatedUser == null || !isAuthPerson) {
            onStopJob(params);
        } else {
            //execute job
            Toast.makeText(getApplicationContext(), R.string.info_background_data_sync_started,Toast.LENGTH_LONG).show();
            handleBackgroundWork(params);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(getClass().getSimpleName(), "mUzima Job Service stopped" + params.getJobId());
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(getClass().getSimpleName(), "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(getClass().getSimpleName(), "Downloading messages in Job");
        return START_NOT_STICKY;
    }

    private void handleBackgroundWork(JobParameters parameters) {
        if (parameters == null) {
            Log.e(getClass().getSimpleName(), "Parameters for job is null");
        } else {
            new SyncSetupConfigTemplatesBackgroundTask().execute();
            new CohortsAndPatientFullDataSyncBackgroundTask().execute();
            new FormDataUploadBackgroundTask().execute();
            new ProcessedTemporaryFormDataCleanUpBackgroundTask().execute();
            new SyncSettinsBackgroundTask().execute();
            if(muzimaSettingController.isClinicalSummaryEnabled()) {
                new SyncAllPatientReportsBackgroundTask().execute();
            }
            new FormMetaDataSyncBackgroundTask().execute();
            new SyncReportDatasetsBackgroundTask().execute();
            new FormTemplateSyncBackgroundTask().execute();
            new DownloadAndDeleteCohortsBasedOnConfigChangesBackgroundTask().execute();
        }
    }

    private class  ProcessedTemporaryFormDataCleanUpBackgroundTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
           new ProcessedTemporaryFormDataCleanUpIntent(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class CohortsAndPatientFullDataSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (new WizardFinishPreferenceService(MuzimaJobScheduler.this).isWizardFinished()) {
                new SyncCohortsAndPatientFullDataIntent(getApplicationContext()).start();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormDataUploadBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (new WizardFinishPreferenceService(getApplicationContext()).isWizardFinished()) {
                RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(),true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormMetaDataSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                if (new WizardFinishPreferenceService(context).isWizardFinished() &&
                        !((MuzimaApplication) context).getFormController().isFormWithPatientDataAvailable(context)) {

                    new SyncFormIntent(getApplicationContext()).start();
                } else {
                    Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not sync form metadata. Incomplete/unsyched forms exist");
                }
            } catch (FormController.FormFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not sync form metadata",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncSettinsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncSettingsIntent(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncAllPatientReportsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncAllPatientReports(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncSetupConfigTemplatesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncSetupConfigurationTemplates(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncReportDatasetsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncReportDatasets(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormTemplateSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                //Get forms in the config
                String configJson = "";
                List<String> formUuids = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = ((MuzimaApplication) context).getSetupConfigurationController().getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> forms = JsonUtils.readAsObjectList(configJson, "$['config']['forms']");
                for (Object form : forms) {
                    net.minidev.json.JSONObject form1 = (net.minidev.json.JSONObject) form;
                    String formUuid = form1.get("uuid").toString();
                    formUuids.add(formUuid);
                }

                DownloadedForms downloadedForms = ((MuzimaApplication) context).getFormController().getAllDownloadedForms();
                List<String> formTemplatesToDeleteUuids = new ArrayList<>();
                List<String> downloadedFormUuids = new ArrayList<>();
                List<String>  formTemplateToDownload= new ArrayList<>();

                //Get forms previously downloaded but not in the updated config
                for(DownloadedForm downloadedForm: downloadedForms){
                    if(!formUuids.contains(downloadedForm.getFormUuid())){
                        formTemplatesToDeleteUuids.add(downloadedForm.getFormUuid());
                    }
                    downloadedFormUuids.add(downloadedForm.getFormUuid());
                }

                //Get Added forms to updated config
                for(String formUuid : formUuids){
                    if(!downloadedFormUuids.contains(formUuid)){
                        formTemplateToDownload.add(formUuid);
                    }
                }

                //Get Forms with Updates
                List<Form> allForms = ((MuzimaApplication) context).getFormController().getAllAvailableForms();
                for (Form form : allForms) {
                    if (form.isUpdateAvailable() && formUuids.contains(form.getUuid())) {
                        formTemplateToDownload.add(form.getUuid());
                    }
                }

                boolean isFormWithPatientDataAvailable = ((MuzimaApplication) context).getFormController().isFormWithPatientDataAvailable(context);

                if(!isFormWithPatientDataAvailable){
                    String[] formsToDownload = formTemplateToDownload.stream().toArray(String[]::new);

                    if(formTemplatesToDeleteUuids.size()>0)
                        ((MuzimaApplication) context).getFormController().deleteFormTemplatesByUUID(formTemplatesToDeleteUuids);

                    if(formTemplateToDownload.size()>0)
                        new SyncFormTemplateIntent(context, formsToDownload).start();
                }else{
                    List<String> formsWithPatientData = new ArrayList<>();

                    CompleteFormsWithPatientData completeFormsWithPatientData = ((MuzimaApplication) context).getFormController().getAllCompleteFormsWithPatientData(context);
                    IncompleteFormsWithPatientData incompleteFormsWithPatientData = ((MuzimaApplication) context).getFormController().getAllIncompleteFormsWithPatientData();

                    for(CompleteFormWithPatientData completeFormWithPatientData : completeFormsWithPatientData){
                        formsWithPatientData.add(completeFormWithPatientData.getFormUuid());
                    }

                    for(IncompleteFormWithPatientData inCompleteFormWithPatientData : incompleteFormsWithPatientData){
                        formsWithPatientData.add(inCompleteFormWithPatientData.getFormUuid());
                    }

                    for(String formTemplateToDeleteUuid : formTemplatesToDeleteUuids) {
                        if (!formsWithPatientData.contains(formTemplateToDeleteUuid)) {
                            //Delete form template
                            ((MuzimaApplication) context).getFormController().deleteFormTemplatesByUUID(Collections.singletonList(formTemplateToDeleteUuid));
                        }
                    }

                    List<String> formsToDownloadUuids = new ArrayList<>();
                    for(String formTemplateUuidToDownload : formTemplateToDownload) {
                        if (!formsWithPatientData.contains(formTemplateUuidToDownload)) {
                            formsToDownloadUuids.add(formTemplateUuidToDownload);

                        }
                    }
                    if(formsToDownloadUuids.size()>0){
                        //Download Templates
                        new SyncFormTemplateIntent(context, formsToDownloadUuids.stream().toArray(String[]::new)).start();
                    }
                }
            } catch (FormController.FormFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not fetch downloaded forms ",e);
            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (FormController.FormDeleteException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete form templates ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class DownloadAndDeleteCohortsBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                //Get cohorts in the config
                String configJson = "";
                List<String> cohortUuids = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = ((MuzimaApplication) context).getSetupConfigurationController().getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> cohorts = JsonUtils.readAsObjectList(configJson, "$['config']['cohorts']");
                for (Object cohort : cohorts) {
                    net.minidev.json.JSONObject cohort1 = (net.minidev.json.JSONObject) cohort;
                    String cohortUuid = cohort1.get("uuid").toString();
                    cohortUuids.add(cohortUuid);
                }

                List<Cohort> syncedCohorts = ((MuzimaApplication) context).getCohortController().getSyncedCohorts();
                List<String> cohortsToSetAsUnsyncedUuids = new ArrayList<>();
                List<String> downloadedCohortUuids = new ArrayList<>();
                List<String> cohortsToDownload= new ArrayList<>();

                //Get cohorts previously downloaded but not in the updated config
                for(Cohort cohort: syncedCohorts){
                    if(!cohortUuids.contains(cohort.getUuid())){
                        cohortsToSetAsUnsyncedUuids.add(cohort.getUuid());
                    }
                    downloadedCohortUuids.add(cohort.getUuid());
                }

                //Get Added cohorts to updated config
                for(String cohortUuid : cohortUuids){
                    if(!downloadedCohortUuids.contains(cohortUuid)){
                        cohortsToDownload.add(cohortUuid);
                    }
                }

                if(cohortsToSetAsUnsyncedUuids.size()>0) {
                    ((MuzimaApplication) context).getCohortController().setSyncStatus(cohortsToSetAsUnsyncedUuids.stream().toArray(String[]::new), 0);
                    ((MuzimaApplication) context).getCohortController().deletePatientsNotBelongingToAnotherCohortByCohortUuids(cohortsToSetAsUnsyncedUuids);
                    ((MuzimaApplication) context).getCohortController().deleteAllCohortMembersByCohortUuids(cohortsToSetAsUnsyncedUuids);

                }

                if(cohortsToDownload.size()>0)
                    new SyncPatientDataIntent(context, cohortsToDownload.stream().toArray(String[]::new)).start();

            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (CohortController.CohortFetchException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not be able to fetch cohort ",e);
            } catch (CohortController.CohortUpdateException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not able to update cohort ",e);
            } catch (CohortController.CohortReplaceException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not able to replace cohort ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}
