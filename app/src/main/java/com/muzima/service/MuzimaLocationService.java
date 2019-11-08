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

    public HashMap<String, Object> getLastKnownGPS() {

        HashMap<String, Object> locationResultMap = new HashMap<>();
        Location location = null;
        if (isOverallLocationAccessPermissionsGranted) {

            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
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
                    locationResultMap.put("gps_location", new MuzimaGPSLocation(location));
                }
                Toast.makeText(context, context.getResources().getString(R.string.hint_switch_location_on), Toast.LENGTH_LONG).show();
            } else {
                isLocationServicesSwitchedOn = true;
                locationResultMap.put("network_provider_status", "switched_on");
                locationResultMap.put("gps_provider_status", "unchecked");
                locationResultMap.put("gps_location", new MuzimaGPSLocation(location));
            }
        } else {
            locationResultMap.put("network_provider_status", "unchecked");
            locationResultMap.put("gps_provider_status", "unchecked");
            locationResultMap.put("gps_location_string", "Permission denied by User");
        }
        return locationResultMap;
    }
}
