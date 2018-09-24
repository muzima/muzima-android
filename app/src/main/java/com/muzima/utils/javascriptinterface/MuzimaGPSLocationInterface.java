package com.muzima.utils.javascriptinterface;

import android.app.Activity;
import android.location.Location;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaLocationService;

public class MuzimaGPSLocationInterface {

    private MuzimaGPSLocation muzimaGPSLocation;
    private String muzimaGPSRepresentation = "";

    @JavascriptInterface
    public String getLastKnowGPSLocation(Activity activity) {
        MuzimaLocationService muzimaLocationService = new MuzimaLocationService(activity.getApplicationContext(), activity);
        Location location = muzimaLocationService.getLastKnownGPS();
        Log.e(getClass().getSimpleName(), "LocationData: " + location.toString());

        muzimaGPSLocation = new MuzimaGPSLocation(location);
        muzimaGPSRepresentation = muzimaGPSLocation.toString();
        Log.e(getClass().getSimpleName(), "LocationData: " + muzimaGPSRepresentation);

        return muzimaGPSRepresentation;
    }
}
