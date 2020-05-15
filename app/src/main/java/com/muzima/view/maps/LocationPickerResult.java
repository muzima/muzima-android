package com.muzima.view.maps;

public class LocationPickerResult {
    private String latitude;
    private String longitude;

    public LocationPickerResult(String latitude, String longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
