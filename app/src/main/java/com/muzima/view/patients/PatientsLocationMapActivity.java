package com.muzima.view.patients;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.util.Constants;
import com.muzima.view.BroadcastListenerActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static java.lang.String.format;

public class PatientsLocationMapActivity extends BroadcastListenerActivity {

    private Button getDirectionsButton;
    private WebView webView;

    private String selectedPatientUuid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patients_location_map);
        initializeHomeLocationMapView();
        initializeMapActionButtons();
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

        webView.addJavascriptInterface(this,"patientsLocationMapInterface");


        webView.loadUrl("file:///android_asset/www/maps/patientsHomeLocationMap.html");
    }

    private void initializeMapActionButtons(){

        getDirectionsButton = findViewById(R.id.getDirections);
        getDirectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Patient patient = getSelectedPatient();
                if (patient != null && patient.getPreferredAddress() != null) {
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
                } else {
                    Log.d(getClass().getSimpleName(), message);
                }
                return true;
            }
        };
    }

    @JavascriptInterface
    public String getCurrentGPSLocation(){
        HashMap locationDataHashMap = ((MuzimaApplication)getApplicationContext()).getMuzimaGPSLocationService().getLastKnownGPS();
        if (locationDataHashMap.containsKey("gps_location")) {
            MuzimaGPSLocation muzimaGPSLocation = ((MuzimaGPSLocation) locationDataHashMap.get("gps_location"));
            try {
                return muzimaGPSLocation.toJsonObject().toString();
            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(), "Error while obtaining GPS location", e);
            }
        }
        return null;
    }
    @JavascriptInterface
    public String getPatientsHomeDetailsForMapping(){
        JSONArray jsonArray = new JSONArray();
        try {
            PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
            List<Patient> patients = patientController.getAllPatients();
            for (Patient patient : patients) {
                JSONObject personAddressObject = new JSONObject();
                PersonAddress personAddress = patient.getPreferredAddress();
                if (personAddress != null) {
                    try {
                        personAddressObject.put("longitude", personAddress.getLongitude());
                        personAddressObject.put("latitude", personAddress.getLatitude());
                        personAddressObject.put("patientSummary", patient.getDisplayName());
                        personAddressObject.put("patientUuid", patient.getUuid());
                        jsonArray.put(personAddressObject);
                    } catch (JSONException e) {
                        Log.e(getClass().getSimpleName(), "Could not get home location", e);
                    }
                }
            }
        }catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(),"Could not plot client locations",e);
        }
        return jsonArray.toString();
    }


    @JavascriptInterface
    public void setSelectedPatientUuid(String uuid){
        selectedPatientUuid = uuid;
    }
    @JavascriptInterface
    public void clearSelectedPatientUuid(){
        selectedPatientUuid = null;
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
    public void navigateToPatientSummary(){
        webView.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(PatientsLocationMapActivity.this, PatientSummaryActivity.class);
                Patient patient = getSelectedPatient();
                intent.putExtra(PatientSummaryActivity.PATIENT, patient);
                startActivity(intent);
            }
        });
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

    private Patient getSelectedPatient() {
        if(selectedPatientUuid != null) {
            try{
            return ((MuzimaApplication) getApplicationContext()).getPatientController().getPatientByUuid(selectedPatientUuid);

            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Could not obtain patient record",e);
            }
        }
        return null;
    }

    private void promptSetLocation(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Client location");
        alertDialog.setMessage("No client location has been set.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.general_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}
