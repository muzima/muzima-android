
package com.muzima;

import android.app.Application;
import android.os.Build;
import com.muzima.api.context.Context;
import com.muzima.api.context.ContextFactory;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.controller.*;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.ConceptPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.util.Constants;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.io.*;
import java.security.Security;

@ReportsCrashes(formKey = "ACRA_FORM_KEY")
public class MuzimaApplication extends Application{
    private Context muzimaContext;
    private FormController formController;
    private CohortController cohortController;
    private PatientController patientConroller;
    private ConceptController conceptController;
    private ObservationController observationController;
    private EncounterController encounterController;
    private MuzimaSyncService muzimaSyncService;
    private CohortPrefixPreferenceService prefixesPreferenceService;

    static {
        // see http://rtyley.github.io/spongycastle/
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private ConceptPreferenceService conceptPreferenceService;

    public void clearApplicationData() {
        try {
            File dir = new File(ContextFactory.APP_DIR);
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
        super.onCreate();
        try {
            ContextFactory.setProperty(Constants.RESOURCE_CONFIGURATION_STRING, getConfigurationString());
            muzimaContext = ContextFactory.createContext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Context getMuzimaContext(){
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

    public FormController getFormController(){
        if(formController == null){
            try {
                formController = new FormController(muzimaContext.getFormService(), muzimaContext.getPatientService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return formController;
    }

    public CohortController getCohortController(){
        if(cohortController == null){
            try {
                cohortController = new CohortController(muzimaContext.getCohortService());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return cohortController;
    }

    public PatientController getPatientController() {
        if(patientConroller == null){
            try {
                patientConroller = new PatientController(muzimaContext.getPatientService(),muzimaContext.getCohortService() );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return patientConroller;
    }

    public ObservationController getObservationController() {
        if(observationController == null){
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
        if(encounterController == null){
            try {
                encounterController = new EncounterController(muzimaContext.getService(EncounterService.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return encounterController;
    }

    public MuzimaSyncService getMuzimaSyncService() {
        if(muzimaSyncService == null){
            muzimaSyncService = new MuzimaSyncService(this);
        }
        return muzimaSyncService;
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

    public CohortPrefixPreferenceService getCohortPrefixesPreferenceService() {
        if(prefixesPreferenceService == null) {
            prefixesPreferenceService = new CohortPrefixPreferenceService(this);
        }
        return prefixesPreferenceService;
    }

    public ConceptPreferenceService getConceptPreferenceService() {
        if (conceptPreferenceService == null) {
            conceptPreferenceService = new ConceptPreferenceService(this);
        }
        return conceptPreferenceService;
    }
}
