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

import com.muzima.model.location.MuzimaGPSLocation;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("MissingPermission")
public class MuzimaLocationService {

    LocationManager locationManager;
    LocationListener locationListener;
    private Boolean isFineGPSLocationAccessGranted = false;
    private Context context;

    private Boolean isNetworkLocationProviderEnabled = false;
    private Boolean isGPSLocationProviderEnabled = false;

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


//        muzimaGPSRepresentation = muzimaGPSLocation.toString();

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
                Toast.makeText(context, "Kindly switch on location in settings.", Toast.LENGTH_LONG).show();
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

    private Location getBestGPSLocation() {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public String getHardGPSData() {
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        return location.toString();
    }

//    protected void onResume(){ TODO apply on resume to calling activity
//        super.onResume();
//        isLocationEnabled();
//    }

    private void isLocationEnabled(String provider) {

        Boolean isEnabled = false;

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle("Enable Location and Internet connectivity.");
            alertDialog.setMessage("You location is switched off! Kindly turn location on in settings.");
            alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                }
            });

            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        } else {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle("Confirm Location");
            alertDialog.setMessage("Your Location is enabled, please enjoy");
            alertDialog.setNegativeButton("Back to interface", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        }
    }

}
