package com.muzima.view.forms;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.service.FormParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_REGISTRATION;
import static com.muzima.utils.Constants.STATUS_COMPLETE;

public class FormDataStore {
    private static final String TAG = "FormDataStore";

    private FormWebViewActivity formWebViewActivity;
    private FormController formController;
    private FormData formData;
    private MuzimaApplication applicationContext;

    public FormDataStore(FormWebViewActivity formWebViewActivity, FormController formController, FormData formData) {
        this.formWebViewActivity = formWebViewActivity;
        this.formController = formController;
        this.formData = formData;
        this.applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();

    }

    @JavascriptInterface
    public void save(String jsonData, String xmlData, String status) {
        Patient newPatient = null;
        if (isRegistrationComplete(status)) {
            newPatient = formController.createNewPatient(jsonData);
            formData.setPatientUuid(newPatient.getUuid());
            formWebViewActivity.startPatientSummaryView(newPatient);
        }
        Log.d(TAG, "xml data is:" + xmlData);
        formData.setXmlPayload(xmlData);
        Log.d(TAG, "json data is:" + jsonData);
        formData.setJsonPayload(jsonData);
        formData.setStatus(status);
        try {
            formController.saveFormData(formData);
            formWebViewActivity.setResult(FormsActivity.RESULT_OK);
            formWebViewActivity.finish();

            FormParser formParser = getFormParser();
            List<Observation> observations = formParser.parseForm(xmlData);

            applicationContext.getObservationController().saveObservations(observations);
        } catch (FormController.FormDataSaveException e) {
            Toast.makeText(formWebViewActivity, "An error occurred while saving the form", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving form data" + e);
        } catch (ObservationController.SaveObservationException e) {
            Toast.makeText(formWebViewActivity, "An error occurred while saving the observations", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving observations data" + e);
        } catch (ConceptController.ConceptFetchException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FormParser getFormParser() {
        return new FormParser(applicationContext.getPatientController(), applicationContext.getConceptController(), applicationContext.getEncounterController(), applicationContext.getObservationController());
    }


    private boolean isRegistrationComplete(String status) {
        return isRegistrationForm() && status.equals(STATUS_COMPLETE);
    }

    @JavascriptInterface
    public String getFormPayload() {
        return formData.getJsonPayload();
    }

    @JavascriptInterface
    public String getFormStatus() {
        return formData.getStatus();
    }

    @JavascriptInterface
    public void showSaveProgressBar() {
        formWebViewActivity.showProgressBar("Saving...");
    }

    public boolean isRegistrationForm() {
        return (formData.getDiscriminator() == null) ? false :
                formData.getDiscriminator().equals(FORM_DISCRIMINATOR_REGISTRATION);
    }
}
