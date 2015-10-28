/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
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
import com.muzima.controller.NotificationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.domain.Credentials;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.util.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.forms.FormWebViewActivity;
import com.muzima.view.forms.HTMLFormWebViewActivity;
import com.muzima.view.preferences.MuzimaTimer;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Security;
import java.util.List;

import static com.muzima.view.preferences.MuzimaTimer.getTimer;

@ReportsCrashes(

        formKey = "",
        reportType = HttpSender.Type.JSON,
        httpMethod = HttpSender.Method.POST,
        formUri = "http://173.255.205.23:5984/acra-muzima/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "muzima-reporter",
        formUriBasicAuthPassword = "OMHKOHV8LVfv3c553n6Oqkof",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)


public class MuzimaApplication extends Application {
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
    private MuzimaTimer muzimaTimer;
    public static final String APP_DIR = "/data/data/com.muzima";
    private SntpService sntpService;
    private User authenticatedUser;

    static {
        // see http://rtyley.github.io/spongycastle/
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
        ACRA.init(this);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Security.removeProvider("AndroidOpenSSL");
        }
        logOut();
        muzimaTimer = getTimer(this);

        super.onCreate();
        try {
            ContextFactory.setProperty(Constants.LUCENE_DIRECTORY_PATH, APP_DIR);
            ContextFactory.setProperty(Constants.RESOURCE_CONFIGURATION_STRING, getConfigurationString());
            muzimaContext = ContextFactory.createContext();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                        muzimaContext.authenticate(username, password, server, NetworkUtils.isConnectedToNetwork(this), false);
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
                conceptController = new ConceptController(muzimaContext.getService(ConceptService.class), muzimaContext.getService(ObservationService.class));
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
                formController = new FormController(muzimaContext.getFormService(), muzimaContext.getPatientService(), muzimaContext.getLastSyncTimeService(), sntpService,
                        muzimaContext.getObservationService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return formController;
    }

    public CohortController getCohortController() {
        if (cohortController == null) {
            try {
                cohortController = new CohortController(muzimaContext.getCohortService(), muzimaContext.getLastSyncTimeService(), getSntpService());
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
                notificationController = new NotificationController(muzimaContext.getService(NotificationService.class), muzimaContext.getFormService());
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

    public void resetTimer(int timeOutInMin) {
        muzimaTimer = muzimaTimer.resetTimer(timeOutInMin);
    }

    public void restartTimer() {
        muzimaTimer.restart();
    }

    public void logOut() {
        saveBeforeExit();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String passwordKey = getResources().getString(R.string.preference_password);
        settings.edit().putString(passwordKey, StringUtil.EMPTY).commit();
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

    private String getConfigurationString() throws IOException {
        InputStream inputStream = getResources().openRawResource(R.raw.configuration);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    public boolean isRunningInBackground() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = manager.getRunningTasks(1);
        return tasks.get(0).topActivity.getClassName().contains("Launcher");
    }
}
