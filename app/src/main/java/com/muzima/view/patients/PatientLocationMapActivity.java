/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;
import android.os.Bundle;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.FormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.util.Constants;
import com.muzima.utils.GeolocationJsonMapper;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.maps.GPSLocationPickerActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static com.muzima.view.maps.GPSLocationPickerActivity.LATITUDE;
import static com.muzima.view.maps.GPSLocationPickerActivity.LONGITUDE;
import static java.text.MessageFormat.format;

public class PatientLocationMapActivity extends BroadcastListenerActivity{
    private static int PICK_LOCATION_REQUEST_CODE = 201;
    private Patient patient;
    Button getDirectionsButton;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_location_map);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        getActionBar().setTitle(patient.getSummary());
        initializeHomeLocationMapView();
        initializeMapActionButtons();

        getLatestPatientRecord();
        if(!patientHomeLocationExists()){
            promptSetLocation();
        }
    }

    private void getLatestPatientRecord(){
        try {
            patient = ((MuzimaApplication) getApplicationContext()).getPatientController().getPatientByUuid(patient.getUuid());
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Could not refresh patient record",e);
        }
    }

    private void initializeMapActionButtons(){

        getDirectionsButton = findViewById(R.id.getDirections);
        getDirectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(patient.getPreferredAddress() != null) {
                    StringBuilder latLngString = new StringBuilder();
                    latLngString.append(patient.getPreferredAddress().getLatitude());
                    latLngString.append("%2C");
                    latLngString.append(patient.getPreferredAddress().getLongitude());

                    Uri mapIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + latLngString.toString());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            }
        });
    }

    @JavascriptInterface
    public void showGetDirectionsButton(){
        webView.post(new Runnable() {
            @Override
            public void run() {
                getDirectionsButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @JavascriptInterface
    public void hideGetDirectionsButton(){
        webView.post(new Runnable() {
            @Override
            public void run() {
                getDirectionsButton.setVisibility(View.GONE);
            }
        });
    }
    @JavascriptInterface
    public String getPatientHomeDetailsForMapping(){
        JSONObject personAddressObject = new JSONObject();
        PersonAddress personAddress = patient.getPreferredAddress();
        if(personAddress != null) {
            try {
                personAddressObject.put("longitude", personAddress.getLongitude());
                personAddressObject.put("latitude", personAddress.getLatitude());
                personAddressObject.put("patientSummary", patient.getDisplayName());
            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(), "Could not get home location", e);
            }
        }
        return personAddressObject.toString();
    }

    @JavascriptInterface
    public String getMapsAPIKey(){
        try {
            MuzimaSetting muzimaSetting = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController()
                    .getSettingByProperty(Constants.ServerSettings.MAPS_API_KEY);
            if(muzimaSetting != null){
                return muzimaSetting.getValueString();
            }
        } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not obtain API key",e);
        }
        return null;
    }

    private void initializeHomeLocationMapView(){

        webView = findViewById(R.id.webview);

        webView.setWebChromeClient(createWebChromeClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }
        webView.addJavascriptInterface(this,"patientLocationMapInterface");
        webView.loadUrl("file:///android_asset/www/maps/patientHomeLocationMap.html");
    }


    private WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = format("Javascript Log. Message: {0}, lineNumber: {1}, sourceId, {2}", consoleMessage.message( ),
                        consoleMessage.lineNumber( ), consoleMessage.sourceId( ));
                if (consoleMessage.messageLevel( ) == ERROR) {
                    Log.e(getClass().getSimpleName(), message);
                }
                return true;
            }
        };
    }

    private boolean patientHomeLocationExists(){
        PersonAddress personAddress = patient.getPreferredAddress();
        return personAddress != null && !StringUtils.isEmpty(personAddress.getLatitude()) &&
                !StringUtils.isEmpty(personAddress.getLongitude());
    }

    private void promptSetLocation(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.title_set_client_location));
        alertDialog.setMessage(getString(R.string.hint_set_client_location));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.general_ok),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(PatientLocationMapActivity.this, GPSLocationPickerActivity.class);
                    startActivityForResult(intent, PICK_LOCATION_REQUEST_CODE);
                }
            });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.general_cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
        alertDialog.show();
    }

    private void promptUpdateLocation(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.title_client_location_update));
        alertDialog.setMessage(getString(R.string.hint_client_location_update));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.general_ok),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(PatientLocationMapActivity.this, GPSLocationPickerActivity.class);
                    if(patientHomeLocationExists()){
                        intent.putExtra(LATITUDE, patient.getPreferredAddress().getLatitude());
                        intent.putExtra(LONGITUDE, patient.getPreferredAddress().getLongitude());
                    }
                    startActivityForResult(intent, PICK_LOCATION_REQUEST_CODE);
                }
            });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.general_cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode ==PICK_LOCATION_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                if(data.hasExtra(GPSLocationPickerActivity.LATITUDE) && data.hasExtra(LONGITUDE)) {

                    try {
                        String latitude = data.getStringExtra(LATITUDE);
                        String longitude = data.getStringExtra(LONGITUDE);

                        PersonAddress preferredAddress = patient.getPreferredAddress();

                        if (preferredAddress == null) {
                            List<PersonAddress> addresses = patient.getAddresses();
                            if(addresses.size() == 1){
                                preferredAddress = addresses.get(0);
                            } else {
                                preferredAddress = new PersonAddress();
                                patient.getAddresses().add(preferredAddress);
                            }
                            preferredAddress.setPreferred(true);
                        }

                        preferredAddress.setLatitude(latitude);
                        preferredAddress.setLongitude(longitude);
                        ((MuzimaApplication) getApplicationContext()).getPatientController().updatePatient(patient);
                        createLocationUpdateFormData();
                        initializeHomeLocationMapView();
                    } catch (PatientController.PatientSaveException e) {
                        Log.e(getClass().getSimpleName(), "Could not update patient locaction", e);
                    }
                } else {
                    finish();
                }
            } else {
                finish();
            }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.geomapping, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.update_client_location) {
            promptUpdateLocation();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }
}
