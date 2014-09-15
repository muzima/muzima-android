/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.actionbarsherlock.app.ActionBar;
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
import com.muzima.utils.Constants;
import com.muzima.utils.audio.AudioResult;
import com.muzima.utils.barcode.IntentIntegrator;
import com.muzima.utils.barcode.IntentResult;
import com.muzima.utils.imaging.ImageResult;
import com.muzima.utils.video.VideoResult;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.utils.Constants.STATUS_COMPLETE;
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;
import static java.text.MessageFormat.format;

public class FormWebViewActivity extends BroadcastListenerActivity {
    private static final String TAG = "FormWebViewActivity";
    public static final String PATIENT = "patient";
    public static final String FORM_INSTANCE = "formInstance";
    public static final String REPOSITORY = "formDataRepositoryContext";
    public static final String BARCODE = "barCodeComponent";
    public static final String IMAGE = "imagingComponent";
    public static final String AUDIO = "audioComponent";
    public static final String VIDEO = "videoComponent";
    public static final String ZIGGY_FILE_LOADER = "ziggyFileLoader";
    public static final String FORM = "form";
    public static final String DISCRIMINATOR = "discriminator";


    private WebView webView;
    private Form form;
    private FormTemplate formTemplate;
    private MuzimaProgressDialog progressDialog;
    private FormData formData;
    private Patient patient;
    private BarCodeComponent barCodeComponent;
    private ImagingComponent imagingComponent;
    private AudioComponent audioComponent;
    private VideoComponent videoComponent;
    private Map<String, String> scanResultMap;
    private Map<String, String> imageResultMap;
    private Map<String, String> audioResultMap;
    private Map<String, String> videoResultMap;
    private String sectionName;
    private FormController formController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        formController = ((MuzimaApplication) this.getApplicationContext()).getFormController();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        scanResultMap = new HashMap<String, String>();
        imageResultMap = new HashMap<String, String>();
        audioResultMap = new HashMap<String, String>();
        videoResultMap = new HashMap<String, String>();
        setContentView(R.layout.activity_form_webview);
        progressDialog = new MuzimaProgressDialog(this);
        showProgressBar("Loading...");
        try {
            setupFormData();
            setupWebView();
        } catch (FormFetchException e) {
            Log.e(TAG, e.getMessage(), e);
            finish();
        } catch (FormController.FormDataFetchException e) {
            Log.e(TAG, e.getMessage(), e);
            finish();
        } catch (FormController.FormDataSaveException e) {
            Log.e(TAG, e.getMessage(), e);
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
        if (isFormComplete() && isEncounterForm()) {
            getSupportMenuInflater().inflate(R.menu.menu_completed_encounter_form, menu);
        } else if (isFormComplete() && !isEncounterForm()) {
            getSupportMenuInflater().inflate(R.menu.menu_completed_registration_form, menu);
        } else {
            getSupportMenuInflater().inflate(R.menu.menu_save_form, menu);
        }
        return true;
    }

    @Override
    protected void onResume() {
        if (scanResultMap != null && !scanResultMap.isEmpty()) {
            String jsonMap = new JSONObject(scanResultMap).toString();
            Log.d(TAG, jsonMap);
            webView.loadUrl("javascript:document.populateBarCode(" + jsonMap + ")");
        }

        if (imageResultMap != null && !imageResultMap.isEmpty()) {
            String jsonMap = new JSONObject(imageResultMap).toString();
            Log.d(TAG, "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populateImage('" + sectionName + "', " + jsonMap + ")");
        }

        if (audioResultMap != null && !audioResultMap.isEmpty()) {
            String jsonMap = new JSONObject(audioResultMap).toString();
            Log.d(TAG, "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populateAudio('" + sectionName + "', " + jsonMap + ")");
        }

        if (videoResultMap != null && !videoResultMap.isEmpty()) {
            String jsonMap = new JSONObject(videoResultMap).toString();
            Log.d(TAG, "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populateVideo('" + sectionName + "', " + jsonMap + ")");
        }

        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.form_save_as_draft:
                saveDraft();
                return true;
            case R.id.form_submit:
                webView.loadUrl("javascript:document.submit()");
                return true;
            case R.id.form_close:
                processBackButtonPressed();
                return true;
            case R.id.form_back_to_draft:
                try {
                    formData.setStatus(STATUS_INCOMPLETE);
                    formController.saveFormData(formData);
                } catch (FormController.FormDataSaveException e) {
                    Log.e(TAG, "Error while saving the form data", e);
                }
                startIncompleteFormListActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void saveDraft() {
        if (!isFormComplete()) {
            webView.loadUrl("javascript:document.saveDraft()");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            scanResultMap.put(barCodeComponent.getFieldName(), scanResult.getContents());
        }

        ImageResult imageResult = ImagingComponent.parseActivityResult(requestCode, resultCode, intent);
        if (imageResult != null)  {
            sectionName =  imageResult.getSectionName();
            imageResultMap.put(imagingComponent.getImagePathField(), imageResult.getImageUri());
            imageResultMap.put(imagingComponent.getImageCaptionField(), imageResult.getImageCaption());
        }

        AudioResult audioResult = AudioComponent.parseActivityResult(requestCode, resultCode, intent);
        if (audioResult != null)  {
            sectionName =  audioResult.getSectionName();
            audioResultMap.put(audioComponent.getAudioPathField(), audioResult.getAudioUri());
            audioResultMap.put(audioComponent.getAudioCaptionField(), audioResult.getAudioCaption());
        }

        VideoResult videoResult = VideoComponent.parseActivityResult(requestCode, resultCode, intent);
        if (videoResult != null)  {
            sectionName =  videoResult.getSectionName();
            videoResultMap.put(videoComponent.getVideoPathField(), videoResult.getVideoUri());
            videoResultMap.put(videoComponent.getVideoCaptionField(), videoResult.getVideoCaption());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(FormWebViewActivity.this);
            builder
                    .setCancelable(true)
                    .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                    .setTitle(getResources().getString(R.string.caution))
                    .setMessage(getResources().getString(R.string.exit_form_message))
                    .setPositiveButton("Yes", positiveClickListener())
                    .setNegativeButton("No", null).create().show();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void startPatientSummaryView(Patient patient) {
        Intent intent = new Intent(this, PatientSummaryActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT, patient);
        startActivity(intent);
    }

    public void startIncompleteFormListActivity() {
        startActivity(new Intent(this, FormsActivity.class));
    }

    private boolean isFormComplete() {
        return formData.getStatus().equalsIgnoreCase(STATUS_COMPLETE);
    }

    private void setupFormData()
            throws FormFetchException, FormController.FormDataFetchException, FormController.FormDataSaveException {
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();

        BaseForm baseForm = (BaseForm) getIntent().getSerializableExtra(FORM);
        form = formController.getFormByUuid(baseForm.getFormUuid());
        patient = (Patient) getIntent().getSerializableExtra(PATIENT);
        formTemplate = formController.getFormTemplateByUuid(baseForm.getFormUuid());

        if (baseForm.hasData()) {
            formData = formController.getFormDataByUuid(((FormWithData) baseForm).getFormDataUuid());
        } else {
            formData = createNewFormData();
        }
    }

    private FormData createNewFormData() throws FormController.FormDataSaveException {
        FormData formData = new FormData() {{
            setUuid(UUID.randomUUID().toString());
            setPatientUuid(patient.getUuid());
            setUserUuid("userUuid");
            setStatus(STATUS_INCOMPLETE);
            setTemplateUuid(form.getUuid());
            setDiscriminator(form.getDiscriminator());
        }};
        try {
            PatientJSONMapper mapper = new PatientJSONMapper(formTemplate.getModelJson());
            formData.setJsonPayload(mapper.map(patient, formData));
        } catch (JSONException e) {
            Log.e(TAG, "Error while converting Model JSON", e);
        }
        return formData;
    }


    private void setupWebView() {
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebChromeClient(createWebChromeClient());

        getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);

        FormInstance formInstance = new FormInstance(form, formTemplate);
        webView.addJavascriptInterface(formInstance, FORM_INSTANCE);
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        webView.addJavascriptInterface(new FormDataStore(this, formController, formData), REPOSITORY);
        barCodeComponent = new BarCodeComponent(this);
        imagingComponent = new ImagingComponent(this);
        audioComponent = new AudioComponent(this);
        videoComponent = new VideoComponent(this);
        webView.addJavascriptInterface(barCodeComponent, BARCODE);
        webView.addJavascriptInterface(imagingComponent, IMAGE);
        webView.addJavascriptInterface(audioComponent, AUDIO);
        webView.addJavascriptInterface(videoComponent, VIDEO);
        webView.addJavascriptInterface(new ZiggyFileLoader("www/ziggy", getApplicationContext().getAssets(), formInstance.getModelJson()), ZIGGY_FILE_LOADER);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        if (isFormComplete()) {
            webView.setOnTouchListener(createCompleteFormListenerToDisableInput());
        }
        webView.loadUrl("file:///android_asset/www/enketo/template.html");
    }

    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                FormWebViewActivity.this.setProgress(progress * 1000);
                if (progress == 100) {
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
        };
    }

    private View.OnTouchListener createCompleteFormListenerToDisableInput() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_MOVE) {
                    view.setFocusable(false);
                    view.setEnabled(false);
                    return true;
                }
                return false;
            }
        };
    }

    private WebSettings getSettings() {
        return webView.getSettings();
    }

    private Dialog.OnClickListener positiveClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                processBackButtonPressed();
            }
        };
    }

    private void processBackButtonPressed() {
        onBackPressed();
    }

    public void showProgressBar(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                progressDialog.show(message);
            }
        });
    }

    private boolean isEncounterForm() {
        return getIntent().getStringExtra(DISCRIMINATOR).equals(Constants.FORM_XML_DISCRIMINATOR_ENCOUNTER)
            || getIntent().getStringExtra(DISCRIMINATOR).equals(Constants.FORM_JSON_DISCRIMINATOR_CONSULTATION);
    }
}

