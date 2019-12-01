package com.muzima.view.patients;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.utils.StringUtils;
import com.muzima.view.BroadcastListenerActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PatientsLocationMapActivity extends BroadcastListenerActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patients_location_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MuzimaGPSLocationService gpsLocationService = ((MuzimaApplication)getApplicationContext()).getMuzimaGPSLocationService();
        if(!gpsLocationService.isGPSLocationPermissionsGranted()){
            gpsLocationService.requestGPSLocationPermissions(this);
        }

        if(!gpsLocationService.isLocationServicesSwitchedOn()){
            gpsLocationService.requestSwitchOnLocation(this);
        }

        if(gpsLocationService.isGPSLocationPermissionsGranted() && gpsLocationService.isLocationServicesSwitchedOn()) {
            HashMap locationDataHashMap = gpsLocationService.getLastKnownGPS();
            if (locationDataHashMap.containsKey("gps_location")) {
                MuzimaGPSLocation muzimaGPSLocation = ((MuzimaGPSLocation) locationDataHashMap.get("gps_location"));
                if(!StringUtils.isEmpty(muzimaGPSLocation.getLatitude()) && !StringUtils.isEmpty(muzimaGPSLocation.getLongitude())) {
                    LatLng latLng = new LatLng(Double.parseDouble(muzimaGPSLocation.getLatitude()),
                            Double.parseDouble(muzimaGPSLocation.getLongitude()));
                    //promptSetLocation();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    googleMap.animateCamera( CameraUpdateFactory.zoomTo( 12.0f ) );
                }
            }
        }

        plotLocations(googleMap);
    }

    private void promptSetLocation(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Client location");
        alertDialog.setMessage("No client location has been set. Do you want to set it now?");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.general_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void plotLocations(GoogleMap googleMap){
        try {
            PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
            List<Patient> patients = patientController.getAllPatients();
            for (Patient patient : patients) {
                PersonAddress personAddress = patient.getPreferredAddress();
                if (personAddress != null && !StringUtils.isEmpty(personAddress.getLatitude()) && !StringUtils.isEmpty(personAddress.getLongitude())) {
                    LatLng latLng = new LatLng(Double.parseDouble(personAddress.getLatitude()), Double.parseDouble(personAddress.getLongitude()));
                    googleMap.addMarker(new MarkerOptions().position(latLng)
                            .title(patient.getDisplayName()));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    googleMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(13.0f));
                }
            }
        }catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(),"Could not plot client locations",e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.geomapping, menu);

        super.onCreateOptionsMenu(menu);
        return true;
    }
}
