package com.muzima.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.model.location.MuzimaGPSLocation;

import java.util.HashMap;

import static com.muzima.utils.Constants.MuzimaGPSLocationConstants.LOCATION_ACCESS_PERMISSION_REQUEST_CODE;

@SuppressWarnings("MissingPermission")
public class MuzimaGPSLocationService {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;

    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;

    @SuppressLint("MissingPermission")
    public MuzimaGPSLocationService(Context context) {
        this.context = context;
        locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);

        if(isGPSLocationPermissionsGranted()) {
            requestLocationUpdates();
        }
    }

    public HashMap<String, Object> getLastKnownGPS() {

        HashMap<String, Object> locationResultMap = new HashMap<>();
        if(!isGPSLocationFeatureEnabled()){
            locationResultMap.put("gps_location_status", "Location Feature disabled");
        } else if (isGPSLocationPermissionsGranted()) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location == null) {
                    locationResultMap.put("gps_provider_status", "switched_off");
                    locationResultMap.put("network_provider_status", "switched_off");
                    locationResultMap.put("gps_location_status", "Location unavailable");
                } else {
                    locationResultMap.put("gps_provider_status", "switched_on");
                    locationResultMap.put("network_provider_status", "switched_on");
                    locationResultMap.put("gps_location", new MuzimaGPSLocation(location));
                }
            } else {
                locationResultMap.put("network_provider_status", "switched_on");
                locationResultMap.put("gps_provider_status", "not_checked");
                locationResultMap.put("gps_location", new MuzimaGPSLocation(location));
            }
        } else {
            locationResultMap.put("gps_location_status", "Permission not granted by User");
        }
        return locationResultMap;
    }

    public boolean isGPSLocationFeatureEnabled(){
        GPSFeaturePreferenceService gpsFeaturePreferenceService = ((MuzimaApplication) context.getApplicationContext()).getGPSFeaturePreferenceService();
        return gpsFeaturePreferenceService.isGPSDataCollectionSettingEnabled();
    }

    public boolean isLocationServicesSwitchedOn() {
        boolean isGPSProviderEnabled = false;
        boolean isNetworkEnabled = false;
        if(locationManager != null){
            isGPSProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        return (isGPSProviderEnabled || isNetworkEnabled);
    }

    public boolean isGPSLocationPermissionsGranted() {
        int permissionStatus = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public void requestGPSLocationPermissions(Activity activity) {
        GPSFeaturePreferenceService gpsFeaturePreferenceService = ((MuzimaApplication) context.getApplicationContext()).getGPSFeaturePreferenceService();
        if(gpsFeaturePreferenceService.isGPSDataCollectionSettingEnabled()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity,
                     new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                     LOCATION_ACCESS_PERMISSION_REQUEST_CODE);
            }
        }
    }

    public void requestSwitchOnLocation(final Activity activity) {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        requestLocationUpdates();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        googleApiClient.connect();
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(getClass().getSimpleName(),"Location error " + connectionResult.getErrorCode());
                    }
                }).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(activity, REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(getClass().getSimpleName(),"Cannot load activity",e);
                        }
                        break;
                }
                }
            });
        }
    }

    private void requestLocationUpdates(){
        if(locationListener == null) {
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
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
}