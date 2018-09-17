package com.muzima.utils.javascriptinterface;

import android.app.Activity;
import android.location.Location;
import android.webkit.JavascriptInterface;

import com.muzima.service.MuzimaLocationService;

public class MuzimaGPSLocationInterface {

    @JavascriptInterface
    public Location getLastKnowGPSLocation(Activity activity){
        MuzimaLocationService muzimaLocationService = new MuzimaLocationService(activity);
        return muzimaLocationService.getLastKnownGPS();
    }
}
