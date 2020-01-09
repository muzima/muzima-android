package com.muzima.view.maps;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.util.Constants;
import com.muzima.utils.StringUtils;
import com.muzima.view.BroadcastListenerActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static java.text.MessageFormat.format;

public class MapLocationPickerActivity extends BroadcastListenerActivity {
    public static String LATITUDE = "Latitude";
    public static String LONGITUDE = "Longitude";
    private String latitude;
    private String longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location_picker);
        if(getIntent().hasExtra(LATITUDE)){
            latitude = getIntent().getStringExtra(LATITUDE);
        }
        if(getIntent().hasExtra(LONGITUDE)){
            longitude = getIntent().getStringExtra(LONGITUDE);
        }
        initializeLocationPickerActionButtons();
        initializeLocationPickerMapView();
    }

    private void initializeLocationPickerActionButtons(){

        Button nextButton = findViewById(R.id.next);
        nextButton.setOnClickListener(useSelectdedLocationButtonListener());

        Button previousButton = findViewById(R.id.previous);
        previousButton.setOnClickListener(cancelButtonListener());
    }

    private void initializeLocationPickerMapView(){

        WebView webView = findViewById(R.id.webview);

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
