package com.muzima.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.model.location.MuzimaGPSLocation;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("MissingPermission")
public class MuzimaLocationService {

    LocationManager locationManager;
    LocationListener locationListener;
    private Context context;

    public static Boolean isOverallLocationAccessPermissionsGranted = false;
    public static Boolean isLocationServicesSwitchedOn = false;

    @SuppressLint("MissingPermission")
    public MuzimaLocationService(Context context) {
        this.context = context;

        ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION);

        locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.e(getClass().getSimpleName(), "New Latitude: " + latitude + "New Longitude: " + longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.e(getClass().getSimpleName(), "GPS location enabled");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.e(getClass().getSimpleName(), "GPS location enabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.e(getClass().getSimpleName(), "GPS location disabled");
            }
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    public HashMap<String, String> getLastKnownGPS(String jsonReturnType) throws Exception {

        HashMap<String, String> locationResultMap = new HashMap<>();

        Log.e(getClass().getSimpleName(), "getLastKnownGPS()");
        String gpsLocationString = "Location is unavailable - Unknown Error";
        Location location = null;
        if (isOverallLocationAccessPermissionsGranted) {

            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);


            Log.e(getClass().getSimpleName(), "getLastKnownGPS() == " + location);


            if (location == null) {
                locationResultMap.put("network_provider_status", "switched_off");
                locationResultMap.put("gps_provider_status", "switched_off");
                locationResultMap.put("gps_location_string", "User offline");

                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location == null) {
                    locationResultMap.put("gps_provider_status", "switched_off");
                    locationResultMap.put("network_provider_status", "switched_off");
                    locationResultMap.put("gps_location_string", "Location switched off");

                    isLocationServicesSwitchedOn = false;
                } else {

                    isLocationServicesSwitchedOn = true;

                    locationResultMap.put("gps_provider_status", "switched_on");
                    locationResultMap.put("network_provider_status", "switched_on");
                    locationResultMap.put("gps_location_string", getGpsRepresentationString(location, jsonReturnType));
                }

                Log.e(getClass().getSimpleName(), "getLastKnownGPS() == " + location);
                Toast.makeText(context, context.getResources().getString(R.string.hint_switch_location_on), Toast.LENGTH_LONG).show();
            } else {
                isLocationServicesSwitchedOn = true;
                locationResultMap.put("network_provider_status", "switched_on");
                locationResultMap.put("gps_provider_status", "unchecked");
                locationResultMap.put("gps_location_string", getGpsRepresentationString(location, jsonReturnType));
            }


            Log.e(getClass().getSimpleName(), "Location " + location);

        } else {
            locationResultMap.put("network_provider_status", "unchecked");
            locationResultMap.put("gps_provider_status", "unchecked");
            locationResultMap.put("gps_location_string", "Permission denied by User");
        }

        Log.e(getClass().getSimpleName(), "getLastKnownGPS() == " + gpsLocationString);


        return locationResultMap;
    }

    public String getGpsRepresentationString(Location location, String jsonReturnType) throws Exception {
        if (jsonReturnType.contains("json-object")) {
            MuzimaGPSLocation muzimaGPSLocation = new MuzimaGPSLocation(location);
            return muzimaGPSLocation.toJsonObject().toString();
        } else if (jsonReturnType.contains("json-array")) {
            MuzimaGPSLocation muzimaGPSLocation = new MuzimaGPSLocation(location);
            return muzimaGPSLocation.toJsonArray().toString();
        } else {
            MuzimaGPSLocation muzimaGPSLocation = new MuzimaGPSLocation(location);
            return muzimaGPSLocation.toJsonArray().toString();
        }

    }

}
