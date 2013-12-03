
package com.muzima;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import com.muzima.api.context.Context;
import com.muzima.api.context.ContextFactory;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.controller.*;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.util.Constants;
import com.muzima.view.forms.FormWebViewActivity;
import com.muzima.view.preferences.MuzimaTimer;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.io.*;
import java.security.Security;

import static com.muzima.view.preferences.MuzimaTimer.getTimer;

@ReportsCrashes(formKey = "ACRA_FORM_KEY")
public class MuzimaApplication extends Application {
    private Context muzimaContext;
    private Activity currentActivity;
    private FormController formController;
    private CohortController cohortController;
    private PatientController patientConroller;
    private ConceptController conceptController;
    private ObservationController observationController;
    private EncounterController encounterController;
    private MuzimaSyncService muzimaSyncService;
    private CohortPrefixPreferenceService prefixesPreferenceService;
    private MuzimaTimer muzimaTimer;
    public static final String APP_DIR = "/data/data/com.muzima";

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
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }


    @Override
    public void onCreate() {
        ACRA.init(this);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
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

    public ConceptController getConceptController() {
        if (conceptController == null) {
            try {
                conceptController = new ConceptController(muzimaContext.getService(ConceptService.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return conceptController;
    }

    public FormController getFormController() {
        if (formController == null) {
            try {
                formController = new FormController(muzimaContext.getFormService(), muzimaContext.getPatientService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return formController;
    }

    public CohortController getCohortController() {
        if (cohortController == null) {
            try {
                cohortController = new CohortController(muzimaContext.getCohortService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return cohortController;
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
                        muzimaContext.getService(EncounterService.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return observationController;
    }

    public EncounterController getEncounterController() {
        if (encounterController == null) {
            try {
                encounterController = new EncounterController(muzimaContext.getService(EncounterService.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return encounterController;
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

    public void setTimeOutInMillis(int value) {
        muzimaTimer.setTimeOutInMillis(value);
    }

    public void restartTimer() {
        muzimaTimer.restart();
    }

    public void logOut() {
        saveBeforeExit();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String passwordKey = getResources().getString(R.string.preference_password);
        settings.edit().putString(passwordKey, StringUtil.EMPTY).commit();
    }

    public void cancelTimer() {
        muzimaTimer.cancel();
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    private void saveBeforeExit() {
        if (currentActivity instanceof FormWebViewActivity) {
            ((FormWebViewActivity) currentActivity).saveDraft();
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
}
