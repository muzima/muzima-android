package com.muzima.model.location;

import android.location.Location;

public class MuzimaGPSLocation {


    private String longitude;
    private String latitude;
    private String timeStamp;
    private String altitude;
    private String speed;
    private String bearing;

    private Location location;

    public MuzimaGPSLocation(final Location location) {
        this.location = location;

        if (location != null){
            intialiseLocationData();
        }
    }

    private void setLongitude() {
        this.longitude = Double.toString(location.getLongitude());
    }

    private void setLatitude() {
        this.latitude = Double.toString(location.getLatitude());
    }

    private void setTimeStamp() {
        this.timeStamp = String.valueOf(location.getTime());
    }

    private void setAltitude() {
        this.altitude = Double.toString(location.getAltitude());
    }

    private void setSpeed() {
        this.speed = String.valueOf(location.getSpeed());
    }

    private void setBearing() {
        this.bearing = String.valueOf(location.getBearing());
    }

    private void intialiseLocationData(){
        setAltitude();
        setBearing();
        setLatitude();
        setLongitude();
        setSpeed();
        setTimeStamp();
    }

    @Override
    public String toString() {
        return "Muzima Location Data : {\n" +
                "\n[ Latitude : " + latitude +
                "]\n Longitude : " + longitude +
                "]\n[ Altitude : " + altitude +
                "]\n[ Timestamp : " + timeStamp +
                "]\n[ Speed : " + speed +
                "]\n[ Bearing : " + bearing +
                "] }";
    }
}
