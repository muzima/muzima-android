package com.muzima.view.patients;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.PatientController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.utils.StringUtils;
import com.muzima.view.BroadcastListenerActivity;

import java.util.HashMap;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientLocationMapActivity extends BroadcastListenerActivity implements OnMapReadyCallback{
    private Patient patient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_location_map);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);

        setupPatientMetadata();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        PersonAddress personAddress = patient.getPreferredAddress();
        if(personAddress != null && !StringUtils.isEmpty(personAddress.getLatitude()) && !StringUtils.isEmpty(personAddress.getLongitude())) {
            LatLng latLng = new LatLng(Double.parseDouble(personAddress.getLatitude()), Double.parseDouble(personAddress.getLongitude()));
            googleMap.addMarker(new MarkerOptions().position(latLng)
                    .title(patient.getDisplayName()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            googleMap.animateCamera( CameraUpdateFactory.zoomTo( 13.0f ) );
        } else {
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

                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//                        googleMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory
//                                .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        googleMap.animateCamera( CameraUpdateFactory.zoomTo( 12.0f ) );


                        promptSetLocation();
                    }
                }
            }


        }
    }

    private void promptSetLocation(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Client location");
        alertDialog.setMessage("No client location has been set. Do you want to set it now?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.general_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                        try {
                            startActivityForResult(builder.build(PatientLocationMapActivity.this), 201);
                        } catch (GooglePlayServicesRepairableException e) {
                            e.printStackTrace();
                        } catch (GooglePlayServicesNotAvailableException e) {
                            e.printStackTrace();
                        }
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.general_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode ==201) {
            if(resultCode == RESULT_OK) {
                try {
                    Place place = PlacePicker.getPlace(this,data);
                    String latitude = String.valueOf(place.getLatLng().latitude);
                    patient.getPreferredAddress().setLatitude(latitude);

                    String longitude = String.valueOf(place.getLatLng().longitude);
                    patient.getPreferredAddress().setLongitude(longitude);
                    try {
                        System.out.println("Updating patint location: lat:"+latitude+", lng:"+longitude);
                        ((MuzimaApplication)getApplicationContext()).getPatientController().updatePatient(patient);
                    } catch (PatientController.PatientSaveException e) {
                        Log.e("Test","Testing",e);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.geomapping, menu);

        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void setupPatientMetadata() {
        TextView patientName = findViewById(R.id.patientName);
        patientName.setText(PatientAdapterHelper.getPatientFormattedName(patient));

        ImageView genderIcon = findViewById(R.id.genderImg);
        int genderDrawable = patient.getGender().equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
        genderIcon.setImageDrawable(getResources().getDrawable(genderDrawable));

        TextView dob = findViewById(R.id.dob);
        dob.setText(String.format("DOB: %s", getFormattedDate(patient.getBirthdate())));

        TextView patientIdentifier = findViewById(R.id.patientIdentifier);
        patientIdentifier.setText(patient.getIdentifier());
    }
}
