/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Location;
import com.muzima.api.service.LocationService;
import com.muzima.service.HTMLLocationParser;
import com.muzima.service.LocationParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LocationController {

    public static final String TAG = "LocationController";
    private LocationService locationService;
    public List<Location> newLocations = new ArrayList<Location>();

    public LocationController(LocationService locationService){
        this.locationService = locationService;
    }

    public List<Location> downloadLocationFromServerByName(String name) throws LocationDownloadException {
        try {
            return locationService.downloadLocationsByName(name);
        } catch (IOException e) {
            Log.e(TAG, "Error while searching for patients in the server", e);
            throw new LocationDownloadException(e);
        }
    }

    public List<Location> downloadLocationsFromServerByName(List<String> names) throws LocationDownloadException {
        HashSet<Location> result = new HashSet<Location>();
        for (String name : names) {
            List<Location> locations = downloadLocationFromServerByName(name);
            result.addAll(locations);
        }
        return new ArrayList<Location>(result);
    }

    public Location downloadLocationFromServerByUuid(String uuid) throws LocationDownloadException {
        try {
            return locationService.downloadLocationByUuid(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Error while searching for patients in the server", e);
            throw new LocationDownloadException(e);
        }
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
            Log.e(TAG, "Error while saving the location : " + location.getUuid(), e);
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

    public Location getLocationByName(String name) throws LocationLoadException  {
        try {
            List<Location> locations = locationService.getLocationsByName(name);
            for (Location location : locations) {
                if (location.getName().equals(name)) {
                    return location;
                }
            }
        } catch (IOException e) {
            throw new LocationLoadException(e);
        }
        catch (org.apache.lucene.queryParser.ParseException e) {
            throw new LocationLoadException(e);
        }
        return null;
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

    public List<Location> getRelatedLocations(List<FormTemplate> formTemplates) throws LocationDownloadException {
        HashSet<Location> locations = new HashSet<Location>();
        LocationParser xmlParserUtils = new LocationParser();
        HTMLLocationParser htmlParserUtils = new HTMLLocationParser();
        for (FormTemplate formTemplate : formTemplates) {
            List<String> names = new ArrayList<String>();
            if (formTemplate.isHTMLForm()) {
                names = htmlParserUtils.parse(formTemplate.getHtml());
            } else {
                // names = xmlParserUtils.parse(formTemplate.getModel());
            }
            locations.addAll(downloadLocationsFromServerByName(names));
        }
        return new ArrayList<Location>(locations);
    }

    public void newLocations(List<Location> locations) throws LocationLoadException {
        newLocations = locations;
        List<Location> savedLocations = getAllLocations();
        newLocations.removeAll(savedLocations);
    }

    public List<Location> newLocations() {
        return newLocations;
    }

    public void deleteAllLocations() throws LocationDeleteException, LocationLoadException {
        deleteLocations(getAllLocations());
    }

    public static class LocationSaveException extends Throwable {
        public LocationSaveException(Throwable throwable) {
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
        public LocationDeleteException(Throwable e) {
            super(e);
        }
    }


}
