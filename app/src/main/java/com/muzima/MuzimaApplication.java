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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.security.ProviderInstaller;
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
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.controller.SmartCardController;
import com.muzima.domain.Credentials;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.dependencies.AxolotlStorageModule;
import com.muzima.messaging.dependencies.SignalCommunicationModule;
import com.muzima.messaging.jobmanager.JobManager;
import com.muzima.messaging.jobmanager.dependencies.DependencyInjector;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.jobs.CreateSignedPreKeyJob;
import com.muzima.messaging.jobs.GcmRefreshJob;
import com.muzima.messaging.jobs.MultiDeviceContactUpdateJob;
import com.muzima.messaging.jobs.PushNotificationReceiveJob;
import com.muzima.messaging.push.SignalServiceNetworkAccess;
import com.muzima.notifications.NotificationChannels;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.ExpiringMessageManager;
import com.muzima.service.LocalePreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.util.Constants;
import com.muzima.util.MuzimaLogger;
import com.muzima.utils.PRNGFixes;
import com.muzima.utils.StringUtils;
import com.muzima.view.forms.FormWebViewActivity;
import com.muzima.view.forms.HTMLFormWebViewActivity;
import com.muzima.view.preferences.MuzimaTimer;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;

import static com.muzima.view.preferences.MuzimaTimer.getTimer;

@ReportsCrashes(

        formKey = "",
        reportType = HttpSender.Type.JSON,
        httpMethod = HttpSender.Method.POST,
        formUri = "http://acra.muzima.org/report",
        formUriBasicAuthLogin = "muzima-reporter",
        formUriBasicAuthPassword = "OMHKOHV8LVfv3c553n6Oqkof",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.hint_crash_dialog,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogTitle = R.string.title_crash_dialog,
        resDialogCommentPrompt = R.string.hint_crash_dialog_comment_prompt,
        resDialogOkToast = R.string.general_thank_you
)


public class MuzimaApplication extends MultiDexApplication implements DependencyInjector, DefaultLifecycleObserver {

    private Context muzimaContext;
    private Activity currentActivity;
    private FormController formController;
    private CohortController cohortController;
    private PatientController patientConroller;
    private ConceptController conceptController;
    private ObservationController observationController;
    private EncounterController encounterController;
    private NotificationController notificationController;
    private LocationController locationController;
    private ProviderController providerController;
    private MuzimaSyncService muzimaSyncService;
    private CohortPrefixPreferenceService prefixesPreferenceService;
    private LocalePreferenceService localePreferenceService;
    private SetupConfigurationController setupConfigurationController;
    private MuzimaSettingController settingsController;
    private SmartCardController smartCardController;
    private MuzimaTimer muzimaTimer;
    private static final String APP_DIR = "/data/data/com.muzima";
    private SntpService sntpService;
    private User authenticatedUser;
    private JobManager jobManager;
    private ObjectGraph objectGraph;
    private volatile boolean isAppVisible = true;
    private ExpiringMessageManager expiringMessageManager;

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
        //ACRA.init(this);
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

        initializeRandomNumberFix();
        initializeDependencyInjection();
        initializeJobManager();
        initializeExpiringMessageManager();
        initializeGcmCheck();
        initializeSignedPreKeyCheck();
        initializeCircumvention();
        initializeWebRtc();
        executePendingContactSync();
        initializePendingMessages();
        NotificationChannels.create(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

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
                formController = new FormController(muzimaContext.getFormService(), muzimaContext.getPatientService(),
                        muzimaContext.getLastSyncTimeService(), getSntpService(), muzimaContext.getObservationService(),
                        muzimaContext.getEncounterService());
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
        if (patientConroller == null) {
            try {
                patientConroller = new PatientController(muzimaContext.getPatientService(), muzimaContext.getCohortService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return patientConroller;
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
                settingsController = new MuzimaSettingController(muzimaContext.getMuzimaSettingService());
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

    public void resetTimer(int timeOutInMin) {
        muzimaTimer = muzimaTimer.resetTimer(timeOutInMin);
    }

    public void restartTimer() {
        muzimaTimer.restart();
    }

    public void logOut() {
        if(authenticatedUser != null) {
            MuzimaLogger.log(getMuzimaContext(), "USER_LOGOUT",
                    "{\"userId\":\"" + authenticatedUser.getUsername() + "\"}");
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

    public static MuzimaApplication getInstance(android.content.Context context) {
        return (MuzimaApplication) context.getApplicationContext();
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    @Override
    public void injectDependencies(Object object) {
        if (object instanceof InjectableType) {
            objectGraph.inject(object);
        }
    }

    private void initializeDependencyInjection() {
        this.objectGraph = ObjectGraph.create(new SignalCommunicationModule(this, new SignalServiceNetworkAccess(this)),
                new AxolotlStorageModule(this));
    }

    public boolean isAppVisible() {
        return isAppVisible;
    }

    private void initializeRandomNumberFix() {
        PRNGFixes.apply();
    }

    public ExpiringMessageManager getExpiringMessageManager() {
        return expiringMessageManager;
    }

    private void initializeExpiringMessageManager() {
        this.expiringMessageManager = new ExpiringMessageManager(this);
    }

    private void initializeJobManager() {
        WorkManager.initialize(this, new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build());

        this.jobManager = new JobManager(this, WorkManager.getInstance());
    }

    private void initializeGcmCheck() {
        if (TextSecurePreferences.isPushRegistered(this)) {
            long nextSetTime = TextSecurePreferences.getGcmRegistrationIdLastSetTime(this) + TimeUnit.HOURS.toMillis(6);

            if (TextSecurePreferences.getGcmRegistrationId(this) == null || nextSetTime <= System.currentTimeMillis()) {
                this.jobManager.add(new GcmRefreshJob(this));
            }
        }
    }

    private void initializeSignedPreKeyCheck() {
        if (!TextSecurePreferences.isSignedPreKeyRegistered(this)) {
            jobManager.add(new CreateSignedPreKeyJob(this));
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void initializeCircumvention() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (new SignalServiceNetworkAccess(MuzimaApplication.this).isCensored(MuzimaApplication.this)) {
                    try {
                        ProviderInstaller.installIfNeeded(MuzimaApplication.this);
                    } catch (Throwable t) {
                        Log.w(getClass().getSimpleName(), t);
                    }
                }
                return null;
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initializePendingMessages() {
        if (TextSecurePreferences.getNeedsMessagePull(this)) {
            MuzimaApplication.getInstance(this).getJobManager().add(new PushNotificationReceiveJob(this));
            TextSecurePreferences.setNeedsMessagePull(this, false);
        }
    }

    private void executePendingContactSync() {
        if (TextSecurePreferences.needsFullContactSync(this)) {
            MuzimaApplication.getInstance(this).getJobManager().add(new MultiDeviceContactUpdateJob(this, true));
        }

    }

    private void initializeWebRtc() {
        try {
            Set<String> HARDWARE_AEC_BLACKLIST = new HashSet<String>() {{
                add("Pixel");
                add("Pixel XL");
                add("Moto G5");
                add("Moto G (5S) Plus");
                add("Moto G4");
                add("TA-1053");
                add("Mi A1");
                add("E5823"); // Sony z5 compact
            }};

            Set<String> OPEN_SL_ES_WHITELIST = new HashSet<String>() {{
                add("Pixel");
                add("Pixel XL");
            }};

            if (HARDWARE_AEC_BLACKLIST.contains(Build.MODEL)) {
                WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
            }

            if (!OPEN_SL_ES_WHITELIST.contains(Build.MODEL)) {
                WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
            }

            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this)
                    .createInitializationOptions());

        } catch (UnsatisfiedLinkError e) {
            throw new AssertionError(e);
        }
    }

}
