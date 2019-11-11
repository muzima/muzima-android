/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.muzima.api.context.Context;
import com.muzima.api.context.ContextFactory;
import com.muzima.api.model.User;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LocationService;
import com.muzima.api.service.NotificationService;
import com.muzima.api.service.ObservationService;
import com.muzima.api.service.ProviderService;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.PatientReportController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.controller.SmartCardController;
import com.muzima.domain.Credentials;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.LocalePreferenceService;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.util.Constants;
import com.muzima.utils.StringUtils;
import com.muzima.view.forms.FormWebViewActivity;
import com.muzima.view.forms.HTMLFormWebViewActivity;
import com.muzima.view.preferences.MuzimaTimer;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;

import static com.muzima.view.preferences.MuzimaTimer.getTimer;

public class MuzimaApplication extends MultiDexApplication {

    private Context muzimaContext;
    private Activity currentActivity;
    private FormController formController;
    private CohortController cohortController;
    private PatientController patientController;
    private ConceptController conceptController;
    private ObservationController observationController;
    private EncounterController encounterController;
    private NotificationController notificationController;
    private LocationController locationController;
    private ProviderController providerController;
    private MuzimaSyncService muzimaSyncService;
    private MuzimaGPSLocationService muzimaGPSLocationService;
    private CohortPrefixPreferenceService prefixesPreferenceService;
    private LocalePreferenceService localePreferenceService;
    private SetupConfigurationController setupConfigurationController;
    private MuzimaSettingController settingsController;
    private SmartCardController smartCardController;
    private PatientReportController patientReportController;
    private MuzimaTimer muzimaTimer;
    private static final String APP_DIR = "/data/data/com.muzima";
    private SntpService sntpService;
    private User authenticatedUser;

    static {
        // see http://rtyley.github.io/spongycastle/
        //TODO There is need to start using Google provided security provider (AndroidOpenSSL)
        //TODO Shipping with both spongycastler  and the default AndroidOpenSSL significantly increases the apk size.
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

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
        //Fabric.with(this, new Crashlytics());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Security.removeProvider("AndroidOpenSSL");
        }
        logOut();
        muzimaTimer = getTimer(this);

        super.onCreate();
        try {
            ContextFactory.setProperty(Constants.LUCENE_DIRECTORY_PATH, APP_DIR);
            muzimaContext = ContextFactory.createContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                        muzimaContext.getService(ObservationService.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return conceptController;
    }

    public ProviderController getProviderController(){
        if(providerController ==  null){
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
                        getSntpService());
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
                patientController = new PatientController(muzimaContext.getPatientService(), muzimaContext.getCohortService(), muzimaContext.getFormService());
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

    public NotificationController getNotificationController() {
        if (notificationController == null) {
            try {
                notificationController = new NotificationController(muzimaContext.getService(NotificationService.class),
                        muzimaContext.getFormService(),this,getSntpService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return notificationController;
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

    public CohortPrefixPreferenceService getCohortPrefixesPreferenceService() {
        if (prefixesPreferenceService == null) {
            prefixesPreferenceService = new CohortPrefixPreferenceService(this);
        }
        return prefixesPreferenceService;
    }

    public LocalePreferenceService getLocalePreferenceService(){
        if(localePreferenceService == null){
            localePreferenceService = new LocalePreferenceService(this);
        }
        return localePreferenceService;
    }

    public SetupConfigurationController getSetupConfigurationController(){
        if(setupConfigurationController == null){
            try {
                setupConfigurationController = new SetupConfigurationController(muzimaContext.getSetupConfigurationService());
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }
        return setupConfigurationController;
    }

    public MuzimaSettingController getMuzimaSettingController() {
        if(settingsController == null){
            try {
                settingsController = new MuzimaSettingController(muzimaContext.getMuzimaSettingService(),
                        muzimaContext.getLastSyncTimeService(), getSntpService());
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
        return settingsController;
    }
    public SmartCardController getSmartCardController() {
        if(smartCardController == null){
            try {
                smartCardController = new SmartCardController(muzimaContext.getSmartCardRecordService());
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
        return smartCardController;
    }

    public PatientReportController getPatientReportController() {
        if (patientReportController == null) {
            try {
                patientReportController = new PatientReportController(muzimaContext.getPatientReportService());
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

    public void resetTimer(int timeOutInMin) {
        muzimaTimer = muzimaTimer.resetTimer(timeOutInMin);
    }

    public void restartTimer() {
        muzimaTimer.restart();
    }

    public void logOut() {
        if(authenticatedUser != null) {
            MuzimaLoggerService.log(getMuzimaContext(), "USER_LOGOUT",
                    getAuthenticatedUserId(), MuzimaLoggerService.getGpsLocation(getApplicationContext()),"{}");
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        saveBeforeExit();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String passwordKey = getResources().getString(R.string.preference_password);
        settings.edit().putString(passwordKey, StringUtils.EMPTY).commit();
        evictAuthenticatedUser();
    }

    public void cancelTimer() {
        muzimaTimer.cancel();
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    private void evictAuthenticatedUser(){
        authenticatedUser = null;
    }

    public String getAuthenticatedUserId(){
        User authenticatedUser = getAuthenticatedUser();
        if(authenticatedUser != null)
            return authenticatedUser.getUsername() != null ? authenticatedUser.getUsername():authenticatedUser.getSystemId();
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
}
