/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.ActionBar;
import android.os.Build;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.BaseForm;
import com.muzima.model.FormWithData;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.utils.Constants;
import com.muzima.utils.GeolocationJsonMapper;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.audio.AudioResult;
import com.muzima.utils.imaging.ImageResult;
import com.muzima.utils.video.VideoResult;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.barcode.BarcodeCaptureActivity;
import com.muzima.view.maps.LocationPickerResult;
import com.muzima.view.progressdialog.MuzimaProgressDialog;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.utils.Constants.STATUS_COMPLETE;
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;
import static com.muzima.view.forms.BarCodeComponent.RC_BARCODE_CAPTURE;
import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;
import static java.text.MessageFormat.format;

public class HTMLFormWebViewActivity extends BroadcastListenerActivity {
    public static final String PATIENT = "patient";
    private static final String FORM_INSTANCE = "formInstance";
    private static final String HTML_DATA_STORE = "htmlDataStore";
    private static final String BARCODE = "barCodeComponent";
    private static final String IMAGE = "imagingComponent";
    private static final String AUDIO = "audioComponent";
    private static final String VIDEO = "videoComponent";
    private static final String GPS_LOCATION_PICKER = "gpsLocationPickerComponent";
    private static final String RELATIONSHIP_CREATOR = "relationshipCreatorComponent";
    private static final String ENCOUNTER_MINI_FORM_CREATOR = "encounterMiniFormCreatorComponent";
    public static final String FORM = "form";
    public static final String DISCRIMINATOR = "discriminator";
    private static final String DEFAULT_AUTO_SAVE_INTERVAL_VALUE_IN_MINS = "2";
    public static final String DEFAULT_FONT_SIZE = "Medium";
    private static final boolean IS_LOGGED_IN_USER_DEFAULT_PROVIDER = false;
    public static final boolean IS_ALLOWED_FORM_DATA_DUPLICATION = true;
    private static final String SAVE_AS_INCOMPLETE = "saveDraft";
    private static final String SAVE_AS_COMPLETED = "submit";
    public static final String POPUP = "popup";

    private WebView webView;
    private Form form;
    private Toolbar toolbar;
    private FormTemplate formTemplate;
    private MuzimaProgressDialog progressDialog;
    private FormData formData;
    private Patient patient;
    private BarCodeComponent barCodeComponent;
    private ImagingComponent imagingComponent;
    private AudioComponent audioComponent;
    private VideoComponent videoComponent;
    private GPSLocationPickerComponent gpsLocationPickerComponent;
    private RelationshipComponent relationshipComponent;
    private Map<String, String> scanResultMap;
    private Map<String, String> imageResultMap;
    private Map<String, String> audioResultMap;
    private Map<String, String> videoResultMap;
    private Map<String, String> gpsLocationPickerResultMap;
    private Map<String, String> relationshipPersonResultMap;
    private String sectionName;
    private FormController formController;
    private String autoSaveIntervalPreference;
    private boolean encounterProviderPreference;
    private final Handler handler = new Handler();
    private boolean isFormReload;
    private EncounterMiniFormCreatorComponent encounterMiniFormCreatorComponent;
    private Patient indexPatient;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);

        formController = ((MuzimaApplication) this.getApplicationContext()).getFormController();

        scanResultMap = new HashMap<>();
        imageResultMap = new HashMap<>();
        audioResultMap = new HashMap<>();
        videoResultMap = new HashMap<>();
        gpsLocationPickerResultMap = new HashMap<>();
        relationshipPersonResultMap = new HashMap<>();

        setContentView(R.layout.activity_form_webview);
        if(getIntent().getSerializableExtra(POPUP) != null) {
            if (getIntent().getSerializableExtra(POPUP).equals(true)) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int height = (int) (displayMetrics.heightPixels * 0.9);
                int width = (int) (displayMetrics.widthPixels * 0.9);
                getWindow().setLayout(width, height);
            }
        }

        setTitle(R.string.general_form);
        progressDialog = new MuzimaProgressDialog(this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        autoSaveIntervalPreference = preferences.getString("autoSaveIntervalPreference", DEFAULT_AUTO_SAVE_INTERVAL_VALUE_IN_MINS);
        encounterProviderPreference = preferences.getBoolean("encounterProviderPreference", IS_LOGGED_IN_USER_DEFAULT_PROVIDER);

        showProgressBar(getString(R.string.hint_loading_progress));
        try {
            setupFormData();
            if (!isFormComplete()) {
                startAutoSaveProcess();
            }
            setupWebView();
        } catch (Throwable t) {
            Log.e(getClass().getSimpleName(), t.getMessage(), t);
        }
        super.onStart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndEnableGPSLocation();
    }

    private void checkAndEnableGPSLocation() {
        MuzimaGPSLocationService gpsLocationService = ((MuzimaApplication)getApplicationContext()).getMuzimaGPSLocationService();

        if(gpsLocationService.isGPSLocationFeatureEnabled()) {
            if (!gpsLocationService.isGPSLocationPermissionsGranted()) {
                gpsLocationService.requestGPSLocationPermissions(this);
            }

            if (!gpsLocationService.isLocationServicesSwitchedOn()) {
                gpsLocationService.requestSwitchOnLocation(this);
            }
        }
    }

    private void startAutoSaveProcess() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    autoSaveForm();
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Error while auto saving the form data", e);
                } finally {
                    handler.postDelayed(this,
                            Integer.parseInt(autoSaveIntervalPreference) *
                                    DateUtils.MILLIS_PER_MINUTE);
                }
            }
        };
        handler.postDelayed(runnable,
                Integer.parseInt(autoSaveIntervalPreference) *
                        DateUtils.MILLIS_PER_MINUTE);
    }

    public void stopAutoSaveProcess() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        stopAutoSaveProcess();
        logFormClosed();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isFormComplete() && isEncounterForm()) {
            getMenuInflater().inflate(R.menu.menu_completed_encounter_form, menu);
        } else if (isFormComplete() && !isEncounterForm()) {
            getMenuInflater().inflate(R.menu.menu_completed_registration_form, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_save_form, menu);
            if (StringUtils.equals(form.getDiscriminator(), Constants.FORM_JSON_DISCRIMINATOR_RELATIONSHIP)) {
                menu.findItem(R.id.form_save_as_draft).setVisible(false);
            }
        }
        return true;
    }

    @Override
    protected void onResume() {

        if (scanResultMap != null && !scanResultMap.isEmpty()) {
            String jsonMap = new JSONObject(scanResultMap).toString();
            Log.d(getClass().getSimpleName(), jsonMap);
            webView.loadUrl("javascript:document.populateBarCode(" + jsonMap + ")");
        }

        if (imageResultMap != null && !imageResultMap.isEmpty()) {
            String jsonMap = new JSONObject(imageResultMap).toString();
            Log.d(getClass().getSimpleName(), "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populateImage('" + sectionName + "', " + jsonMap + ")");
        }

        if (audioResultMap != null && !audioResultMap.isEmpty()) {
            String jsonMap = new JSONObject(audioResultMap).toString();
            Log.d(getClass().getSimpleName(), "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populateAudio('" + sectionName + "', " + jsonMap + ")");
        }

        if (videoResultMap != null && !videoResultMap.isEmpty()) {
            String jsonMap = new JSONObject(videoResultMap).toString();
            Log.d(getClass().getSimpleName(), "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populateVideo('" + sectionName + "', " + jsonMap + ")");
        }

        if (gpsLocationPickerResultMap != null && !gpsLocationPickerResultMap.isEmpty()) {
            String jsonMap = new JSONObject(gpsLocationPickerResultMap).toString();
            Log.d(getClass().getSimpleName(), "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populatePickedGpsLocation('" + sectionName + "', " + jsonMap + ")");
        }

        if (relationshipComponent != null && !relationshipPersonResultMap.isEmpty()) {
            String jsonMap = new JSONObject(relationshipPersonResultMap).toString();
            Log.d(getClass().getSimpleName(), "Header:" + sectionName + "json:" + jsonMap);
            webView.loadUrl("javascript:document.populateRelationshipPerson('" + sectionName + "', " + jsonMap + ")");
        }
        super.onResume();
        languageUtil.onResume(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.form_save_as_draft:
                saveDraft();
                return true;
            case R.id.form_submit:
                if (StringUtils.equals(form.getDiscriminator(), Constants.FORM_JSON_DISCRIMINATOR_RELATIONSHIP)) {
                    saveMiniForm();
                } else {
                    saveCompleted();
                }
                return true;
            case R.id.form_close:
                processBackButtonPressed();
                return true;
            case android.R.id.home:
                showAlertDialog();
                return true;
            case R.id.form_back_to_draft:
                try {
                    formController.markFormDataAsIncompleteAndDeleteRelatedEncountersAndObs(formData);
                } catch (FormController.FormDataSaveException | FormController.FormDataDeleteException e) {
                    Log.e(getClass().getSimpleName(), "Error while saving the form data", e);
                }
                restartWebViewActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAlertDialog() {
        new AlertDialog.Builder(HTMLFormWebViewActivity.this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.general_caution))
                .setMessage(getResources().getString(R.string.warning_form_close))
                .setPositiveButton(getString(R.string.general_yes), positiveClickListener())
                .setNegativeButton(getString(R.string.general_no), null)
                .create()
                .show();
    }

    public void showWarningDialog(String saveType) {
        new AlertDialog.Builder(HTMLFormWebViewActivity.this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_duplicate_form_data_warning))
                .setMessage(getResources().getString(R.string.warning_form_data_already_exists))
                .setPositiveButton(getString(R.string.confirm_duplicate_form_data_save), duplicateFormDataClickListener(saveType))
                .setNegativeButton(getString(R.string.general_cancel), null)
                .create()
                .show();
    }

    public void showWarningDialog() {
        new AlertDialog.Builder(HTMLFormWebViewActivity.this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_duplicate_form_data_warning))
                .setMessage(getResources().getString(R.string.warning_form_data_already_exists))
                .setNegativeButton(getString(R.string.general_ok), null)
                .create()
                .show();
    }

    private Dialog.OnClickListener duplicateFormDataClickListener(final String saveType) {

        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (saveType.equals(SAVE_AS_INCOMPLETE)) {
                    webView.loadUrl("javascript:document.saveDraft()");
                } else if (saveType.equals(SAVE_AS_COMPLETED)) {
                    webView.loadUrl("javascript:document.submit()");
                }
            }
        };
    }

    private void autoSaveForm() {
        webView.loadUrl("javascript:document.autoSaveForm()");
    }

    private void saveDraft() {
        webView.loadUrl("javascript:document.saveDraft()");
    }

    private void saveCompleted() {
        webView.loadUrl("javascript:document.submit()");
    }

    private void saveMiniForm() {
        webView.loadUrl("javascript:document.saveMiniForm()");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (intent != null) {
                    Barcode barcode = intent.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    scanResultMap.put(barCodeComponent.getFieldName(), barcode.displayValue);
                } else {
                    Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null");
                }
            } else {
                Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null "+CommonStatusCodes.getStatusCodeString(resultCode));
            }
        }

        ImageResult imageResult = ImagingComponent.parseActivityResult(requestCode, resultCode, intent);
        if (imageResult != null) {
            sectionName = imageResult.getSectionName();
            imageResultMap.put(imagingComponent.getImagePathField(), imageResult.getImageUri());
            imageResultMap.put(imagingComponent.getImageCaptionField(), imageResult.getImageCaption());
        }

        AudioResult audioResult = AudioComponent.parseActivityResult(requestCode, resultCode, intent);
        if (audioResult != null) {
            sectionName = audioResult.getSectionName();
            audioResultMap.put(audioComponent.getAudioPathField(), audioResult.getAudioUri());
            audioResultMap.put(audioComponent.getAudioCaptionField(), audioResult.getAudioCaption());
        }

        VideoResult videoResult = VideoComponent.parseActivityResult(requestCode, resultCode, intent);
        if (videoResult != null) {
            sectionName = videoResult.getSectionName();
            videoResultMap.put(videoComponent.getVideoPathField(), videoResult.getVideoUri());
            videoResultMap.put(videoComponent.getVideoCaptionField(), videoResult.getVideoCaption());
        }

        LocationPickerResult locationPickerResult = GPSLocationPickerComponent.parseActivityResult(requestCode, resultCode, intent);
        if (locationPickerResult != null) {
            sectionName = gpsLocationPickerComponent.getSectionName();
            gpsLocationPickerResultMap.put(gpsLocationPickerComponent.getLatitudeField(), locationPickerResult.getLatitude());
            gpsLocationPickerResultMap.put(gpsLocationPickerComponent.getLongitudeField(), locationPickerResult.getLongitude());
            if(gpsLocationPickerComponent.isCreateDemographicsUpdatePreferred()){
                createLocationUpdateFormData();
            }
        }

        CreateRelationshipPersonResult relationshipPersonResult = RelationshipComponent.parseActivityResult(requestCode, resultCode, intent);
        if (relationshipPersonResult != null) {
            sectionName = relationshipComponent.getSectionName();
            relationshipPersonResultMap.put(relationshipComponent.getPersonUuidField(), relationshipPersonResult.getPersonUuid());
        }
    }

    private void createLocationUpdateFormData(){
        try {
            new GeolocationJsonMapper(patient, (MuzimaApplication) getApplicationContext()).createAndSaveLocationUpdateFormData();
        } catch (FormController.FormDataSaveException e) {
            Log.e(getClass().getSimpleName(), "Could not create location Update formData",e);
            Toast.makeText(this, R.string.error_geolocation_update_failure,Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not create location Update formData",e);
            Toast.makeText(this, R.string.error_geolocation_update_failure,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showAlertDialog();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void startPatientSummaryView(Patient patient) {
        Intent intent = new Intent(this, PatientSummaryActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patient.getUuid());
        startActivity(intent);
    }

    private void restartWebViewActivity() {
        startActivity(getIntent());
        finish();
    }

    private boolean isFormComplete() {
        return formData != null && formData.getStatus().equalsIgnoreCase(STATUS_COMPLETE);
    }

    private void setupFormData()
            throws FormFetchException, FormController.FormDataFetchException {
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        BaseForm baseForm = (BaseForm) getIntent().getSerializableExtra(FORM);
        form = formController.getFormByUuid(baseForm.getFormUuid());
        patient = (Patient) getIntent().getSerializableExtra(PATIENT);
        indexPatient = (Patient) getIntent().getSerializableExtra(INDEX_PATIENT);
        formTemplate = formController.getFormTemplateByUuid(baseForm.getFormUuid());

        if (baseForm.hasData()) {
            formData = formController.getFormDataByUuid(((FormWithData) baseForm).getFormDataUuid());
            isFormReload = true;
        } else {
            createNewFormData();
            isFormReload = false;
        }
    }

    private void createNewFormData() {
        formData = new FormData() {{
            setUuid(UUID.randomUUID().toString());
            setPatientUuid(patient.getUuid());
            setUserSystemId(((MuzimaApplication) getApplicationContext()).getAuthenticatedUser().getSystemId());
            setUserUuid("userUuid");
            setStatus(STATUS_INCOMPLETE);
            setTemplateUuid(form.getUuid());
            setDiscriminator(form.getDiscriminator());
        }};

        if (isGenericRegistrationForm() || isDemographicsUpdateForm() || isPersonRegistrationForm() || isPersonUpdateForm()) {
            formData.setJsonPayload(new GenericPatientRegistrationJSONMapper().map(((MuzimaApplication) getApplicationContext()),patient, formData, encounterProviderPreference, indexPatient));
        } else {
            formData.setJsonPayload(new HTMLPatientJSONMapper().map(((MuzimaApplication) getApplicationContext()), patient, formData, encounterProviderPreference));
        }
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);
        webView.setWebChromeClient(createWebChromeClient());
        getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }

        FormInstance formInstance = new FormInstance(form, formTemplate);
        webView.addJavascriptInterface(formInstance, FORM_INSTANCE);
        barCodeComponent = new BarCodeComponent(this);
        imagingComponent = new ImagingComponent(this);
        audioComponent = new AudioComponent(this);
        videoComponent = new VideoComponent(this);

        gpsLocationPickerComponent = new GPSLocationPickerComponent(this);
        relationshipComponent = new RelationshipComponent(this, patient);

        encounterMiniFormCreatorComponent = new EncounterMiniFormCreatorComponent(this);

        webView.addJavascriptInterface(barCodeComponent, BARCODE);
        webView.addJavascriptInterface(imagingComponent, IMAGE);
        webView.addJavascriptInterface(audioComponent, AUDIO);
        webView.addJavascriptInterface(videoComponent, VIDEO);

        webView.addJavascriptInterface(gpsLocationPickerComponent, GPS_LOCATION_PICKER);
        webView.addJavascriptInterface(relationshipComponent, RELATIONSHIP_CREATOR);

        webView.addJavascriptInterface(encounterMiniFormCreatorComponent, ENCOUNTER_MINI_FORM_CREATOR);

        webView.addJavascriptInterface(new HTMLFormDataStore(this, formData, isFormReload,
                (MuzimaApplication) getApplicationContext()), HTML_DATA_STORE);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        if (isFormComplete()) {
            webView.setOnTouchListener(createCompleteFormListenerToDisableInput());
        }
        webView.loadDataWithBaseURL("file:///android_asset/www/forms/", prePopulateData(),
                "text/html", "UTF-8", "");
    }

    private String prePopulateData() {
        if (formData.getJsonPayload() == null) {
            return formTemplate.getHtml();
        }
        Document document = Jsoup.parse(formTemplate.getHtml());
        String json = formData.getJsonPayload();
        String htmlWithJSON = "<div id='pre_populate_data'>" + json + "</div>";
        document.select("body").prepend(htmlWithJSON);
        return document.toString();
    }

    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                HTMLFormWebViewActivity.this.setProgress(progress * 1000);
                if (progress == 100) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = format("Javascript Log. Message: {0}, lineNumber: {1}, sourceId, {2}", consoleMessage.message(),
                        consoleMessage.lineNumber(), consoleMessage.sourceId());
                if (consoleMessage.messageLevel() == ERROR) {
                    Log.e(getClass().getSimpleName(), message);
                } else {
                    Log.d(getClass().getSimpleName(), message);
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
        handler.removeCallbacksAndMessages(null);
        onBackPressed();
    }

    private void showProgressBar(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                progressDialog.show(message);
            }
        });
    }

    private boolean isDemographicsUpdateForm() {
        return formController.isDemographicsUpdateFormData(formData);
    }

    private boolean isEncounterForm() {
        return formController.isEncounterFormData(formData);
    }

    private boolean isGenericRegistrationForm() {
        return formController.isGenericRegistrationHTMLFormData(formData);
    }

    private boolean isPersonRegistrationForm(){
        return formController.isPersonRegistrationHTMLFormData(formData);
    }

    private boolean isPersonUpdateForm(){
        return formController.isPersonUpdateHTMLFormData(formData);
    }

    public Handler getHandler() {
        return handler;
    }

    public void showMissingEncounterDetailsDialog(String message) {
        new AlertDialog.Builder(HTMLFormWebViewActivity.this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_missing_form_encounter_details_error))
                .setMessage(message)
                .setNegativeButton(getString(R.string.general_ok), null)
                .create()
                .show();
    }

    private void logFormClosed(){
        try {
            JSONObject eventDetails = new JSONObject();
            if(formData != null) {
                eventDetails.put("patientuuid", formData.getPatientUuid());
                eventDetails.put("formDataUuid", formData.getUuid());
                eventDetails.put("formDiscriminator", formData.getDiscriminator());
            }
            logEvent("FORM_CLOSED",eventDetails.toString());
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(),"Cannot create log",e);
        }
    }
}

