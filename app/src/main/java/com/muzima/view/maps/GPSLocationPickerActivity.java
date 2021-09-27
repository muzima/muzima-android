/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.maps;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
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
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.util.Constants;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import org.json.JSONException;
import org.json.JSONObject;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static com.muzima.service.MuzimaGPSLocationService.REQUEST_LOCATION;
import static com.muzima.utils.Constants.MuzimaGPSLocationConstants.LOCATION_ACCESS_PERMISSION_REQUEST_CODE;
import static java.text.MessageFormat.format;

public class GPSLocationPickerActivity extends BroadcastListenerActivity {
    public static String LATITUDE = "Latitude";
    public static String LONGITUDE = "Longitude";
    public static String DEFAULT_ZOOM_LEVEL = "DefaultZoomLevel";
    private String latitude;
    private String longitude;
    private int defaultZoomLevel;
    private MuzimaGPSLocationService gpsLocationService;
    private final LanguageUtil languageUtil = new LanguageUtil();

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location_picker);
        if(getIntent().hasExtra(LATITUDE)){
            latitude = getIntent().getStringExtra(LATITUDE);
        }
        if(getIntent().hasExtra(LONGITUDE)){
            longitude = getIntent().getStringExtra(LONGITUDE);
        }

        defaultZoomLevel = getIntent().getIntExtra(DEFAULT_ZOOM_LEVEL,12);
        checkAndRequestGPSPermissions();
    }

    @Override
    protected void onResume(){
        super.onResume();

        languageUtil.onResume(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Could not obtain the current GPS location.", Toast.LENGTH_LONG).show();
                latitude = "0.5117";
                longitude = "35.282614";
            } else {
                getLastKnowntGPSLocation();
            }

            initializeLocationPickerActionButtons();
            initializeLocationPickerMapView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //ToDo: Pass request code as parameter to gpslocationservice
        if (requestCode == LOCATION_ACCESS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndRequestGPSPermissions();
            } else {
                Toast.makeText(this,"Could not obtain the current GPS location.",Toast.LENGTH_LONG).show();
                latitude = "0.5117";
                longitude = "35.282614";

                initializeLocationPickerActionButtons();
                initializeLocationPickerMapView();
            }
        }
    }

    private void checkAndRequestGPSPermissions(){
        gpsLocationService = ((MuzimaApplication)getApplicationContext()).getMuzimaGPSLocationService();
        if (!gpsLocationService.isGPSLocationPermissionsGranted()) {
            gpsLocationService.requestGPSLocationPermissions(this, true);
        } else{
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(android.location.Location location) {
                    webView.loadUrl("javascript:document.updateCurrentLocationAndAccuracy(" +
                            location.getLatitude() + ", " + location.getLongitude() + "," + location.getAccuracy() +")");
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            if (!gpsLocationService.isLocationServicesSwitchedOn()) {
                gpsLocationService.requestSwitchOnLocation(this,locationListener);
            } else {
                gpsLocationService.requestLocationUpdates(locationListener);
                getLastKnowntGPSLocation();
                initializeLocationPickerActionButtons();
                initializeLocationPickerMapView();
            }
        }
    }

    private void getLastKnowntGPSLocation(){
        MuzimaGPSLocation muzimaGPSLocation = gpsLocationService.getLastKnownGPSLocation();
        if(muzimaGPSLocation != null){
            latitude = muzimaGPSLocation.getLatitude();
            longitude = muzimaGPSLocation.getLongitude();
        }
    }

    private void initializeLocationPickerActionButtons(){

        Button nextButton = findViewById(R.id.next);
        nextButton.setOnClickListener(useSelectdedLocationButtonListener());

        Button previousButton = findViewById(R.id.previous);
        previousButton.setOnClickListener(cancelButtonListener());
    }

    private void initializeLocationPickerMapView(){

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

        webView.addJavascriptInterface(this,"locationPickerInterface");
        webView.loadUrl("file:///android_asset/www/maps/mapLocationPicker.html");
    }

    @JavascriptInterface
    public void updateSelectedLocation(String latitude, String longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @JavascriptInterface
    public int getDefaultZoomLevel(){
        return defaultZoomLevel;
    }

    @JavascriptInterface
    public String getCurrentGPSLocation(){
        if(!StringUtils.isEmpty(latitude) && !StringUtils.isEmpty(longitude)){
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("latitude", latitude);
                jsonObject.put("longitude", longitude);
                return jsonObject.toString();
            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(), "Error building Json Object for location details",e);
            }
        }

        MuzimaGPSLocation gpsLocation = ((MuzimaApplication)getApplicationContext()).getMuzimaGPSLocationService().getLastKnownGPSLocation();
        try {
            if(gpsLocation != null) {
                return gpsLocation.toJsonObject().toString();
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while obtaining GPS location", e);
        }
        return null;
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

    private View.OnClickListener cancelButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            }
        };
    }

    private View.OnClickListener useSelectdedLocationButtonListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(LATITUDE, latitude);
                resultIntent.putExtra(LONGITUDE, longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        };
    }
}
