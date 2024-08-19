/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.muzima.api.context.Context;
import com.muzima.api.context.ContextFactory;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Credential;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Person;
import com.muzima.api.model.Provider;
import com.muzima.api.model.User;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LocationService;
import com.muzima.api.service.NotificationService;
import com.muzima.api.service.NotificationTokenService;
import com.muzima.api.service.ObservationService;
import com.muzima.api.service.PersonService;
import com.muzima.api.service.PersonTagService;
import com.muzima.api.service.ProviderService;
import com.muzima.controller.AppUsageLogsController;
import com.muzima.controller.AppReleaseController;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FCMTokenController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MediaCategoryController;
import com.muzima.controller.MediaController;
import com.muzima.controller.MinimumSupportedAppVersionController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.PatientReportController;
import com.muzima.controller.PersonController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.RelationshipController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.controller.SmartCardController;
import com.muzima.db.MuzimaDatabase;
import com.muzima.domain.Credentials;
import com.muzima.model.PassphraseStorage;
import com.muzima.service.FormDuplicateCheckPreferenceService;
import com.muzima.service.LocalePreferenceService;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.util.Constants;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.view.forms.FormWebViewActivity;
import com.muzima.view.forms.HTMLFormWebViewActivity;
import com.muzima.view.preferences.MuzimaTimer;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;

import static com.muzima.utils.Constants.STATUS_COMPLETE;
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;
import static com.muzima.view.preferences.MuzimaTimer.getTimer;

import org.apache.lucene.queryParser.ParseException;

public class MuzimaApplication extends MultiDexApplication {

    private Context muzimaContext;
    private Activity currentActivity;
    private FormController formController;
    private CohortController cohortController;
    private PatientController patientController;
    private ConceptController conceptController;
    private ObservationController observationController;
    private EncounterController encounterController;
    private LocationController locationController;
    private ProviderController providerController;
    private MuzimaSyncService muzimaSyncService;
    private MuzimaGPSLocationService muzimaGPSLocationService;
    private LocalePreferenceService localePreferenceService;
    private SetupConfigurationController setupConfigurationController;
    private MuzimaSettingController settingsController;
    private SmartCardController smartCardController;
    private PatientReportController patientReportController;
    private RelationshipController relationshipController;
    private PersonController personController;
    private MinimumSupportedAppVersionController minimumSupportedAppVersionController;
    private FCMTokenController fcmTokenController;
    private ReportDatasetController reportDatasetController;
    private AppUsageLogsController appUsageLogsController;
    private DerivedConceptController derivedConceptController;
    private DerivedObservationController derivedObservationController;
    private MuzimaTimer muzimaTimer;
    private static final String APP_DIR = "/data/data/com.muzima";
    private SntpService sntpService;
    private User authenticatedUser;
    private AppReleaseController appVersionController;
    private MediaController mediaController;
    private MediaCategoryController mediaCategoryController;
    private ExecutorService executorService;
    private FormDuplicateCheckPreferenceService formDuplicateCheckPreferenceService;
    private PassphraseStorage passphraseStorage;
    private MuzimaDatabase database;

    public void clearApplicationData() {
        try {
            File dir = new File(APP_DIR);
            if (dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear the application data", e);
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else {
            return false;
        }
    }

    @Override
    public void onCreate() {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);

        logOut();
        muzimaTimer = getTimer(this);

        super.onCreate();
        checkAndSetLocaleToDeviceLocaleIFDisclaimerNotAccepted();

        try {
            ContextFactory.setProperty(Constants.LUCENE_DIRECTORY_PATH, APP_DIR);
            muzimaContext = ContextFactory.createContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());
    }

    MuzimaDatabase getDatabase() throws Exception{
        if (database == null) {
            database = MuzimaDatabase.getDatabase(this, getPassphraseStorage().getPassphrase());
        }
        return database;
    }

    private PassphraseStorage getPassphraseStorage(){
        if(passphraseStorage == null){
            passphraseStorage = new PassphraseStorage(getApplicationContext());
        }
        return passphraseStorage;
    }

    public void checkAndSetLocaleToDeviceLocaleIFDisclaimerNotAccepted() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        boolean disclaimerAccepted = settings.getBoolean(disclaimerKey, false);
        if (!disclaimerAccepted) {
            String localeKey = getResources().getString(R.string.preference_app_language);
            settings.edit().putString(localeKey, Locale.getDefault().getLanguage()).commit();
        }
    }

    public Context getMuzimaContext() {
        return muzimaContext;
    }

    public User getAuthenticatedUser() {
        try {
            if (authenticatedUser == null) {
                muzimaContext.openSession();
                if (muzimaContext.isAuthenticated())
                    authenticatedUser = muzimaContext.getAuthenticatedUser();
                else {
                    Credentials cred = new Credentials(getApplicationContext());
                    String[] credentials = cred.getCredentialsArray();
                    String username = credentials[0];
                    String password = credentials[1];
                    String server = credentials[2];

                    if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password) && !StringUtils.isEmpty(server)) {
                        muzimaContext.authenticate(username, password, server, false);
                    }

                    authenticatedUser = muzimaContext.getAuthenticatedUser();
                }
                muzimaContext.closeSession();
            }
        } catch (Exception e) {
            muzimaContext.closeSession();
            throw new RuntimeException(e);
        }
        return authenticatedUser;
    }

    public ConceptController getConceptController() {
        if (conceptController == null) {
            try {
                conceptController = new ConceptController(muzimaContext.getService(ConceptService.class),
                        muzimaContext.getService(ObservationService.class), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return conceptController;
    }

    public ProviderController getProviderController() {
        if (providerController == null) {
            try {
                providerController = new ProviderController(muzimaContext.getService(ProviderService.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return providerController;
    }

    public FormController getFormController() {
        if (formController == null) {
            try {
                formController = new FormController(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return formController;
    }

    public CohortController getCohortController() {
        if (cohortController == null) {
            try {
                cohortController = new CohortController(muzimaContext.getCohortService(), muzimaContext.getLastSyncTimeService(),
                        getSntpService(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return cohortController;
    }

    public SntpService getSntpService() {
        if (sntpService == null) {
            sntpService = new SntpService();
        }
        return sntpService;
    }

    public PatientController getPatientController() {
        if (patientController == null) {
            try {
                patientController = new PatientController(muzimaContext.getPatientService(), muzimaContext.getCohortService(), muzimaContext.getFormService(), muzimaContext.getPatientTagService(), muzimaContext.getObservationService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return patientController;
    }

    public ObservationController getObservationController() {
        if (observationController == null) {
            try {
                observationController = new ObservationController(muzimaContext.getObservationService(),
                        muzimaContext.getService(ConceptService.class),
                        muzimaContext.getService(EncounterService.class),
                        muzimaContext.getLastSyncTimeService(),
                        getSntpService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return observationController;
    }

    public EncounterController getEncounterController() {
        if (encounterController == null) {
            try {
                encounterController = new EncounterController(muzimaContext.getService(EncounterService.class),
                        muzimaContext.getLastSyncTimeService(), getSntpService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return encounterController;
    }

    public LocationController getLocationController() {
        if (locationController == null) {
            try {
                locationController = new LocationController(muzimaContext.getService(LocationService.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return locationController;
    }

    public MuzimaSyncService getMuzimaSyncService() {
        if (muzimaSyncService == null) {
            muzimaSyncService = new MuzimaSyncService(this);
        }
        return muzimaSyncService;
    }

    public LocalePreferenceService getLocalePreferenceService() {
        if (localePreferenceService == null) {
            localePreferenceService = new LocalePreferenceService(this);
        }
        return localePreferenceService;
    }

    public SetupConfigurationController getSetupConfigurationController() {
        if (setupConfigurationController == null) {
            try {
                setupConfigurationController = new SetupConfigurationController(muzimaContext.getSetupConfigurationService(),
                        muzimaContext.getLastSyncTimeService(), getSntpService(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return setupConfigurationController;
    }

    public MuzimaSettingController getMuzimaSettingController() {
        if (settingsController == null) {
            try {
                settingsController = new MuzimaSettingController(muzimaContext.getMuzimaSettingService(),
                        muzimaContext.getLastSyncTimeService(), getSntpService(), muzimaContext.getSetupConfigurationService(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return settingsController;
    }

    public SmartCardController getSmartCardController() {
        if (smartCardController == null) {
            try {
                smartCardController = new SmartCardController(muzimaContext.getSmartCardRecordService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return smartCardController;
    }

    public PatientReportController getPatientReportController() {
        if (patientReportController == null) {
            try {
                patientReportController = new PatientReportController(muzimaContext.getPatientReportService(), muzimaContext.getLastSyncTimeService(), getSntpService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return patientReportController;
    }

    public MuzimaGPSLocationService getMuzimaGPSLocationService() {
        if (muzimaGPSLocationService == null) {
            muzimaGPSLocationService = new MuzimaGPSLocationService(this);
        }
        return muzimaGPSLocationService;
    }

    public FormDuplicateCheckPreferenceService getFormDuplicateCheckPreferenceService() {
        if (formDuplicateCheckPreferenceService == null) {
            formDuplicateCheckPreferenceService = new FormDuplicateCheckPreferenceService(this);
        }
        return formDuplicateCheckPreferenceService;
    }

    public RelationshipController getRelationshipController() {
        if (relationshipController == null) {
            try {
                relationshipController = new RelationshipController(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return relationshipController;
    }

    public PersonController getPersonController() {
        if (personController == null) {
            try {
                personController = new PersonController(muzimaContext.getService(PersonService.class), muzimaContext.getPersonTagService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return personController;
    }

    public void resetTimer(int timeOutInMin) {
        muzimaTimer = muzimaTimer.resetTimer(timeOutInMin);
    }

    public void restartTimer() {
        muzimaTimer.restart();
    }

    public void logOut() {
        if (authenticatedUser != null) {
            MuzimaLoggerService.stopLogsSync();
            MuzimaLoggerService.log(this, "USER_LOGOUT", "{}");
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        saveBeforeExit();

        if(muzimaContext != null && getMuzimaSettingController().isOnlineOnlyModeEnabled()){
            deleteAllPatientsData();
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String passwordKey = getResources().getString(R.string.preference_password);
        settings.edit().putString(passwordKey, StringUtils.EMPTY).commit();
        evictAuthenticatedUser();
    }

    public void deleteAllPatientsData(){
        List<Concept> allConcepts = new ArrayList<>();
        try {
            allConcepts = getConceptController().getConcepts();
        } catch (ConceptController.ConceptFetchException e){
            Log.e(getClass().getSimpleName(),"Could not fetch concepts",e);
        }
        if(!allConcepts.isEmpty()) {
            try {
                getObservationController().deleteAllObservations(allConcepts);
            } catch (ObservationController.DeleteObservationException e){
                Log.e(getClass().getSimpleName(),"Could not delete observations",e);
            }
        }

        try {
            List<Encounter> encounters = getEncounterController().getAllEncounters();
            getEncounterController().deleteEncounters(encounters);
        } catch (EncounterController.DeleteEncounterException e) {
            Log.e(getClass().getSimpleName(),"Could not fetch encounters to be deleted",e);
        } catch (EncounterController.FetchEncounterException e) {
            Log.e(getClass().getSimpleName(),"Could not delete encounters",e);
        }

        try {
            List<Cohort> syncedCohorts = getCohortController().getSyncedCohorts();
            if (syncedCohorts.size() > 0) {
                List<String> cohortUuids = new ArrayList<>();
                syncedCohorts.forEach((cohort) -> {
                    try {
                        getCohortController().deleteAllCohortMembers(cohort.getUuid());
                    } catch (CohortController.CohortReplaceException e) {
                        Log.e(getClass().getSimpleName(),"Could not delete cohort members",e);
                    }
                    cohortUuids.add(cohort.getUuid());
                });
                getCohortController().setSyncStatus(cohortUuids, 0);
            }
        } catch (CohortController.CohortUpdateException e) {
            Log.e(getClass().getSimpleName(),"Could not update cohorts",e);
        } catch (CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(),"Could not fetch synced cohorts",e);
        }

        try {
            getPatientController().deleteAllPatients();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),"Could not delete patients",e);
        }

        try {
            getRelationshipController().deleteAllRelationships();
        } catch (RelationshipController.DeleteRelationshipException e) {
            Log.e(getClass().getSimpleName(),"Could not delete relationships",e);
        }

        try{
            List<Provider> providers = getProviderController().getAllProviders();
            List<Person> nonPatientPersons = new ArrayList<>();

            for (Provider provider : providers){
                nonPatientPersons.add(provider.getPerson());
            }

            User authenticated = getAuthenticatedUser();

            if(authenticated != null &&  authenticated.getPerson() != null)
                nonPatientPersons.add(authenticated.getPerson());

            List<Person> availablePersons = getPersonController().getAllPersons();
            for(Person person : availablePersons){
                if(nonPatientPersons.contains(person)){
                    availablePersons.remove(person);
                }
            }
            getPersonController().deletePersons(availablePersons);

            List<FormData> formDataList = new ArrayList<>();
            List<FormData> incompleteForms = getFormController().getAllFormData(STATUS_INCOMPLETE);
            List<FormData> completeForms = getFormController().getAllFormData(STATUS_COMPLETE);
            if(incompleteForms.size()>0)
                formDataList.addAll(incompleteForms);
            if(completeForms.size()>0)
                formDataList.addAll(completeForms);

            getFormController().deleteCompleteAndIncompleteFormData(formDataList);

        } catch (PersonController.PersonLoadException e) {
            Log.e(getClass().getSimpleName(),"Could not load persons for deletion",e);
        } catch (ProviderController.ProviderLoadException e) {
            Log.e(getClass().getSimpleName(),"Could not load providers",e);
        } catch (PersonController.PersonDeleteException e) {
            Log.e(getClass().getSimpleName(),"Could not delete persons",e);
        } catch (FormController.FormDataDeleteException e) {
            Log.e(getClass().getSimpleName(),"Could not delete complete and incomplete forms",e);
        } catch (FormController.FormDataFetchException e) {
            Log.e(getClass().getSimpleName(),"Could not fetch complete and incomplete forms",e);
        }

        try {
            muzimaContext.getLastSyncTimeService().deleteAllPatientDataLastSyncTime();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),"Could not delete lastSyncTime",e);
        }
    }

    public void cancelTimer() {
        muzimaTimer.cancel();
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    private void evictAuthenticatedUser() {
        authenticatedUser = null;
    }

    public String getAuthenticatedUserId() { //might need to delete this block ; no caller
        User authenticatedUser = getAuthenticatedUser();
        if (authenticatedUser != null)
            return authenticatedUser.getUsername() != null ? authenticatedUser.getUsername() : authenticatedUser.getSystemId();
        return "null";
    }

    private void saveBeforeExit() {
        if (currentActivity instanceof FormWebViewActivity) {
            ((FormWebViewActivity) currentActivity).saveDraft();
        }
        if (currentActivity instanceof HTMLFormWebViewActivity) {
            ((HTMLFormWebViewActivity) currentActivity).stopAutoSaveProcess();
        }
    }

    public boolean isRunningInBackground() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        List<ActivityManager.RunningTaskInfo> tasks = manager.getRunningTasks(1);
        return tasks.get(0).topActivity.getClassName().contains("Launcher");
    }

    public MinimumSupportedAppVersionController getMinimumSupportedVersionController() {
        if (minimumSupportedAppVersionController == null) {
            try {
                minimumSupportedAppVersionController = new MinimumSupportedAppVersionController(muzimaContext.getMinimumSupportedAppVersionService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return minimumSupportedAppVersionController;
    }

    public ExecutorService getExecutorService() {
        if (executorService == null) {
            return new ThreadPoolExecutor(
                    20, 20, 3000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(128)
            );
        }
        return executorService;
    }

    public FCMTokenController getFCMTokenController() {
        if (fcmTokenController == null) {
            try {
                fcmTokenController = new FCMTokenController(muzimaContext.getService(NotificationTokenService.class), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fcmTokenController;
    }

    public ReportDatasetController getReportDatasetController() {
        if(reportDatasetController == null){
            try {
                reportDatasetController = new ReportDatasetController(muzimaContext.getReportDatasetService(), muzimaContext.getLastSyncTimeService(), getSntpService());
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
        return reportDatasetController;
    }

    public AppUsageLogsController getAppUsageLogsController(){
        if(appUsageLogsController == null){
            try{
                appUsageLogsController = new AppUsageLogsController(muzimaContext.getAppUsageLogsService());
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        return appUsageLogsController;
    }

    public AppReleaseController getAppReleaseController() {
        if (appVersionController == null) {
            try {
                appVersionController = new AppReleaseController(muzimaContext.getAppReleaseService(), muzimaContext.getLastSyncTimeService(), getSntpService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return appVersionController;
    }

    public MediaCategoryController getMediaCategoryController() {
        if (mediaCategoryController == null) {
            try {
                mediaCategoryController = new MediaCategoryController(muzimaContext.getMediaCategoryService(), muzimaContext.getLastSyncTimeService(), getSntpService(), muzimaContext.getSetupConfigurationService(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return mediaCategoryController;
    }

    public MediaController getMediaController() {
        if (mediaController == null) {
            try {
                mediaController = new MediaController(muzimaContext.getMediaService(), muzimaContext.getLastSyncTimeService(), getSntpService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return mediaController;
    }

    public DerivedConceptController getDerivedConceptController(){
        if(derivedConceptController == null){
            try{
                derivedConceptController = new DerivedConceptController(muzimaContext.getDerivedConceptService());
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        return derivedConceptController;
    }

    public DerivedObservationController getDerivedObservationController(){
        if(derivedObservationController == null){
            try{
                derivedObservationController = new DerivedObservationController(muzimaContext.getDerivedObservationService(), muzimaContext.getLastSyncTimeService(), getSntpService());
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        return derivedObservationController;
    }

    public String getApplicationVersion() {
        String versionText = "";
        String versionCode = "";
        try {
            versionCode = String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            LanguageUtil languageUtil = new LanguageUtil();
            android.content.Context localizedContext = languageUtil.getLocalizedContext(this);
            versionText = localizedContext.getResources().getString(R.string.general_application_version, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(getClass().getSimpleName(), "Unable to read application version.", e);
        }
        return versionText;
    }

    public boolean isNewUser(String username){
        try {
            User user = muzimaContext.getUserService().getUserByUsername(username);

            if(user == null){
                return true;
            }
        }  catch (IOException e) {
            Log.e(getClass().getSimpleName(),"Encountered IO Exception ",e);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Encountered Parse Exception ",e);
        }
        return false;
    }

    public void deleteUserByUserName(String username){
        try {
            User user = muzimaContext.getUserService().getUserByUsername(username);
            Credential credential = muzimaContext.getUserService().getCredentialByUsername(username);
            if(user != null) {
                muzimaContext.getUserService().deleteUser(user);
                muzimaContext.getUserService().deleteCredential(credential);
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),"Encountered IO Exception ",e);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Encountered Parse Exception ",e);
        }
    }

}
