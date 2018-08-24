package com.muzima.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Location;
import com.muzima.api.model.LocationAttribute;
import com.muzima.api.model.LocationAttributeType;
import com.muzima.controller.LocationController;
import com.muzima.utils.Constants.Shr.KenyaEmr;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocationUtils {
    private static String getLocationAttributeValue(Location location, String locationAttributeType){
        if(location != null){
            LocationAttribute locationAttribute = location.getAttribute(locationAttributeType);
            if(locationAttribute != null){
                return locationAttribute.getAttribute();
            }
        }
        return null;
    }

    public static String getKenyaEmrMasterFacilityListCode(Location location){
        String facilityCode = null;
        if(location != null){
            facilityCode = LocationUtils.getLocationAttributeValue(location, KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.name);
            if(StringUtils.isEmpty(facilityCode)){
                facilityCode = LocationUtils.getLocationAttributeValue(location, KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.uuid);
            }
        }
        return facilityCode;
    }

    public static Location getOrCreateDummyLocationByKenyaEmrMasterFacilityListCode(MuzimaApplication muzimaApplication, String facilityCode) throws Exception {
        LocationController locationController = muzimaApplication.getLocationController();
        Location location = null;
        LocationAttributeType locationAttributeType = null;
        try {
            locationAttributeType = locationController.getLocationAttributeTypeByUuid(KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.uuid);

            if (locationAttributeType != null) {
                location = locationController.getLocationByAttributeTypeAndValue(locationAttributeType, facilityCode);
            } else {
                boolean locationFound = false;
                for(Location location1:locationController.getAllLocations()){
                    for(LocationAttribute locationAttribute: location1.getAttributes()){
                        if(StringUtils.equalsIgnoreCase(locationAttribute.getAttributeType().getUuid(),
                                KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.uuid)){
                            location = location1;
                            locationFound = true;
                            break;
                        }
                    }
                    if(locationFound){
                        break;
                    }
                }
            }
        } catch (LocationController.LocationLoadException e){
            Log.e("Location Utils", "Failed to get location",e);
        }

        if(location == null){
            location = new Location();
            location.setName("MFL " + facilityCode);
            location.setUuid(UUID.randomUUID().toString());
            try {
                int id = Integer.parseInt(facilityCode) + 1000000;
                location.setId(id);
            } catch (NumberFormatException e) {
                location.setId(1000000);
            }

            if(locationAttributeType == null){
                locationAttributeType = new LocationAttributeType();
                locationAttributeType.setUuid(KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.uuid);
                locationAttributeType.setName(KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.name);
            }

            LocationAttribute locationAttribute = new LocationAttribute();
            locationAttribute.setAttribute(facilityCode);
            locationAttribute.setAttributeType(locationAttributeType);
            locationAttribute.setUuid(UUID.randomUUID().toString());
            location.addAttribute(locationAttribute);
            try {
                locationController.saveLocation(location);
            } catch (LocationController.LocationSaveException e) {
                throw new Exception("Cannot save newly created identifier",e);
            }
        }
        return location;
    }

    public static Location getDefaultEncounterLocationPreference(MuzimaApplication muzimaApplication) throws Exception {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(muzimaApplication);

            String defaultLocationName = preferences.getString("defaultEncounterLocation", null);
            List<Location> defaultLocation = new ArrayList<>();
            List<Location> locations = muzimaApplication.getLocationController().getAllLocations();
            if (defaultLocationName != null && locations != null) {
                for (Location loc : locations) {
                    if (Integer.toString(loc.getId()).equals(defaultLocationName)) {
                        return loc;
                    }
                }
            }
            return null;
        } catch (LocationController.LocationLoadException e) {
            throw new Exception("Cannot retrieve locations",e);
        }
    }
}
