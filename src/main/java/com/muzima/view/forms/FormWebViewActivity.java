package com.muzima.view.forms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.BaseForm;
import com.muzima.model.FormWithData;
import com.muzima.view.BroadcastListenerActivity;
import org.json.JSONException;

import java.util.UUID;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;
import static java.text.MessageFormat.format;

public class FormWebViewActivity extends BroadcastListenerActivity {
    private static final String TAG = "FormWebViewActivity";
    public static final String PATIENT = "patient";
    public static final String FORM_INSTANCE = "formInstance";
    public static final String REPOSITORY = "formDataRepositoryContext";
    public static final String ZIGGY_FILE_LOADER = "ziggyFileLoader";
    public static final String FORM = "form";


    private WebView webView;
    private Form form;
    private FormTemplate formTemplate;
    private MuzimaProgressDialog progressDialog;
    private FormData formData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_webview);
        progressDialog = new MuzimaProgressDialog(this);
        progressDialog.show("Loading... ");
        try {
            Patient patient = (Patient) getIntent().getSerializableExtra(PATIENT);
            setupFormData(patient);
            setupWebView();
        } catch (FormFetchException e) {
            Log.e(TAG, e.getMessage());
            finish();
        } catch (FormController.FormDataFetchException e) {
            Log.e(TAG, e.getMessage());
            finish();
        } catch (FormController.FormDataSaveException e) {
            Log.e(TAG, e.getMessage());
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.form_save_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.form_save_as_draft:
                webView.loadUrl("javascript:document.saveDraft()");
                return true;
            case R.id.form_submit:
                webView.loadUrl("javascript:document.submit()");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupFormData(Patient patient) throws FormFetchException, FormController.FormDataFetchException, FormController.FormDataSaveException {
        BaseForm formObject = (BaseForm) getIntent().getSerializableExtra(FORM);

        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        String formId = formObject.getFormUuid();
        form = formController.getFormByUuid(formId);
        formTemplate = formController.getFormTemplateByUuid(formId);

        if (formObject.hasData()) {
            formData = formController.getFormDataByUuid(((FormWithData) formObject).getFormDataUuid());
        } else {
            formData = createNewFormData(patient.getUuid(), formId, patient,formTemplate);
        }
    }

    private FormData createNewFormData(final String patientUuid, final String formUuid, Patient patient, FormTemplate formTemplate) throws FormController.FormDataSaveException {
        FormData formData = new FormData() {{
            setUuid(UUID.randomUUID().toString());
            setPatientUuid(patientUuid);
            setUserUuid("userUuid");
            setStatus(STATUS_INCOMPLETE);
            setTemplateUuid(formUuid);
        }};
        try {
            PatientJSONMapper mapper = new PatientJSONMapper(formTemplate.getModelJson());
            formData.setPayload(mapper.map(patient));
        } catch (JSONException e) {
            Log.e(TAG, "Error while converting Model JSON");
        }
        return formData;
    }


    private void setupWebView() {
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                FormWebViewActivity.this.setProgress(progress * 1000);
                if (progress == 100){
                    progressDialog.dismiss();
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = format("Javascript Log. Message: {0}, lineNumber: {1}, sourceId, {2}", consoleMessage.message(),
                        consoleMessage.lineNumber(), consoleMessage.sourceId());
                if (consoleMessage.messageLevel() == ERROR) {
                    Log.e(TAG, message);
                } else {
                    Log.d(TAG, message);
                }
                return true;
            }
        });

        getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);

        FormInstance formInstance = new FormInstance(form, formTemplate);
        webView.addJavascriptInterface(formInstance, FORM_INSTANCE);
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        webView.addJavascriptInterface(new FormDataStore(this, formController, formData), REPOSITORY);
        webView.addJavascriptInterface(new ZiggyFileLoader("www/ziggy", getApplicationContext().getAssets(), formInstance.getModelJson()), ZIGGY_FILE_LOADER);
        webView.addJavascriptInterface(new MuzimaProgressDialog(this), "progressDialog");
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.loadUrl("file:///android_asset/www/enketo/template.html");
    }

    private WebSettings getSettings() {
        return webView.getSettings();
    }

}

