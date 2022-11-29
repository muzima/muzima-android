/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.geomapping.view.maps;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import com.muzima.geomapping.view.maps.LocationPickerResult;
import com.muzima.geomapping.view.maps.GPSLocationPickerActivity;

import static com.muzima.geomapping.view.maps.GPSLocationPickerActivity.LATITUDE;
import static com.muzima.geomapping.view.maps.GPSLocationPickerActivity.LONGITUDE;

public class GPSLocationPickerComponent {

    private static final int PICK_LOCATION_REQUEST_CODE = 0x0000201;
    private final Activity activity;

    private String sectionName;
    private String latitudeField;
    private String longitudeField;
    private boolean createDemographicsUpdatePreferred;

    public GPSLocationPickerComponent(Activity activity){
        this.activity = activity;
    }

    @JavascriptInterface
    public void getGPSLocationPicker(String sectionName, String latitudeField, String longitudeField,
                                     String zoomLevelStr, boolean createDemographicsUpdatePreferred){
        this.sectionName = sectionName;
        this.latitudeField = latitudeField;
        this.longitudeField = longitudeField;
        this.createDemographicsUpdatePreferred = createDemographicsUpdatePreferred;
        int zoomLevel = Integer.valueOf(zoomLevelStr);

        if(zoomLevel < 9 ){
            zoomLevel = 9;
        } else if(zoomLevel > 23){
            zoomLevel = 23;
        }

        Intent intent = new Intent(activity, GPSLocationPickerActivity.class);
        intent.putExtra(GPSLocationPickerActivity.DEFAULT_ZOOM_LEVEL,zoomLevel);
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

    public boolean isCreateDemographicsUpdatePreferred() {
        return createDemographicsUpdatePreferred;
    }
}
