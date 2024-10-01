/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.Location;
import com.muzima.api.model.LocationAttribute;
import com.muzima.api.model.LocationAttributeType;
import com.muzima.api.service.LocationService;
import com.muzima.utils.StringUtils;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LocationController {

    private final LocationService locationService;
    private List<Location> newLocations = new ArrayList<>();

    public LocationController(LocationService locationService){
        this.locationService = locationService;
    }

    public List<Location> downloadLocationFromServerByName(String name) throws LocationDownloadException {
        try {
            return locationService.downloadLocationsByName(name);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for patients in the server", e);
            throw new LocationDownloadException(e);
        }
    }

    public Location downloadLocationFromServerByUuid(String uuid) throws LocationDownloadException {
        try {
            return locationService.downloadLocationByUuid(uuid);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for patients in the server", e);
            throw new LocationDownloadException(e);
        }
    }

    public List<Location> downloadLocationsFromServerByUuid(String[] uuids) throws LocationDownloadException {
        HashSet<Location> result = new HashSet<>();
        try {
            for(String uuid : uuids) {
                 Location location = locationService.downloadLocationByUuid(uuid);
                if(location != null) result.add(location);
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for patients in the server", e);
            throw new LocationDownloadException(e);
        }
        return new ArrayList<>(result);
    }

    public List<Location> getAllLocations() throws LocationLoadException {
        try {
            return locationService.getAllLocations();
        } catch (IOException e) {
            throw new LocationLoadException(e);
        }
    }

    public void saveLocation(Location location) throws LocationSaveException {
        try {
            locationService.saveLocation(location);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the location : " + location.getUuid(), e);
            throw new LocationSaveException(e);
        }
    }

    public void saveLocations(List<Location> locations) throws LocationSaveException {
        try {
            locationService.saveLocations(locations);
        } catch (IOException e) {
            throw new LocationSaveException(e);
        }
    }

    public Location getLocationByUuid(String uuid) throws LocationLoadException {
        try {
            return locationService.getLocationByUuid(uuid);
        } catch (IOException e) {
            throw new LocationLoadException(e);
        }
    }

    public LocationAttributeType getLocationAttributeTypeByUuid(String uuid) throws LocationLoadException {
        try {
            return locationService.getLocationAttributeTypeByUuid(uuid);
        } catch (IOException | ParseException e) {
            throw new LocationLoadException(e);
        }
    }

    public Location getLocationByAttributeTypeAndValue(LocationAttributeType attributeType,String attribute) throws LocationLoadException {
        try {
            List<Location> locations = locationService.getAllLocations();
            for(Location location:locations){
                LocationAttribute locationAttribute = location.getAttribute(attributeType.getName());
                if(locationAttribute != null && StringUtils.equalsIgnoreCase(locationAttribute.getAttribute(),attribute)){
                    return location;
                }

                locationAttribute = location.getAttribute(attributeType.getUuid());
                if(locationAttribute != null && StringUtils.equalsIgnoreCase(locationAttribute.getAttribute(),attribute)){
                    return location;
                }
            }
            return null;
        } catch (IOException e) {
            throw new LocationLoadException(e);
        }
    }

    public Location getLocationById(int id) throws LocationLoadException  {
        try {
            return locationService.getLocationById(id);
        } catch (IOException | ParseException e) {
            throw new LocationLoadException(e);
        }
    }

    public void deleteLocation(Location location) throws LocationDeleteException {
        try {
            locationService.deleteLocation(location);
        } catch (IOException e) {
            throw new LocationDeleteException(e);
        }
    }

    public void deleteLocations(List<Location> locations) throws LocationDeleteException {
        try {
            locationService.deleteLocations(locations);
        } catch (IOException e) {
            throw new LocationDeleteException(e);
        }

    }

    public void deleteLocationsByUuids(List<String> locationUuids) throws LocationDeleteException, IOException {
        List<Location> locations = new ArrayList<>();
        for(String locationUuid : locationUuids){
            Location location = locationService.getLocationByUuid(locationUuid);
            if(location != null){
                locations.add(location);
            }
        }

        if(locations.size() > 0){
            deleteLocations(locations);
        }
    }

    public List<Location> newLocations() {
        return newLocations;
    }

    public void deleteAllLocations() throws LocationDeleteException, LocationLoadException {
        deleteLocations(getAllLocations());
    }

    public static class LocationSaveException extends Throwable {
        LocationSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class LocationDownloadException extends Throwable {
        public LocationDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class LocationLoadException extends Throwable {
        public LocationLoadException(Throwable e) {
            super(e);
        }
    }

    public static class LocationDeleteException extends Throwable {
        LocationDeleteException(Throwable e) {
            super(e);
        }
    }


}
