package com.muzima.view.forms;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.PatientController;
import com.muzima.view.maps.LocationPickerResult;
import com.muzima.view.maps.MapLocationPickerActivity;
import com.muzima.view.patients.PatientLocationMapActivity;

import static com.muzima.view.maps.MapLocationPickerActivity.LATITUDE;
import static com.muzima.view.maps.MapLocationPickerActivity.LONGITUDE;

public class GPSLocationPickerComponent {

    private static final int PICK_LOCATION_REQUEST_CODE = 0x0000201;
    private final Activity activity;

    private String sectionName;
    private String latitudeField;
    private String longitudeField;

    public GPSLocationPickerComponent(Activity activity){
        this.activity = activity;
    }

    @JavascriptInterface
    public void getGPSLocationPicker(String sectionName, String latitudeField, String longitudeField){
        this.sectionName = sectionName;
        this.latitudeField = latitudeField;
        this.longitudeField = longitudeField;
        Intent intent = new Intent(activity, MapLocationPickerActivity.class);
        intent.putExtra(MapLocationPickerActivity.DEFAULT_ZOOM_LEVEL,12);
        activity.startActivityForResult(intent, PICK_LOCATION_REQUEST_CODE);
    }

    public static LocationPickerResult parseActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PICK_LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if(intent.hasExtra(LATITUDE) && intent.hasExtra(LONGITUDE)) {
                String latitude = intent.getStringExtra(LATITUDE);
                String longitude = intent.getStringExtra(LONGITUDE);
                return new LocationPickerResult(latitude,longitude);
            }
        }
        return null;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getLatitudeField() {
        return latitudeField;
    }

    public String getLongitudeField() {
        return longitudeField;
    }
}
