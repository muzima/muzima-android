package com.muzima.utils;

import com.muzima.api.model.Location;
import com.muzima.api.model.LocationAttribute;
import com.muzima.api.model.LocationAttributeType;
import com.muzima.controller.LocationController;
import com.muzima.utils.Constants.Shr.KenyaEmr;

import java.util.UUID;

public class LocationUtils {
    public static String getLocationAttributeValue(Location location, String locationAttributeType){
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

    public static Location getOrCreateDummyLocationByKenyaEmrMasterFacilityListCode(LocationController locationController, String facilityCode) throws LocationController.LocationLoadException {
        LocationAttributeType locationAttributeType = locationController.getLocationAttributeByUuid(KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.uuid);
        Location location = null;

        if(locationAttributeType != null){
            location = locationController.getLocationByAttributeType(locationAttributeType, facilityCode);
        }
        if(location == null){
            location = new Location();
            location.setName("MFL " + facilityCode);
            location.setUuid(UUID.randomUUID().toString());

            if(locationAttributeType == null){
                locationAttributeType = new LocationAttributeType();
                locationAttributeType.setUuid(KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.uuid);
                locationAttributeType.setName(KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.name);
            }

            LocationAttribute locationAttribute = new LocationAttribute();
            locationAttribute.setAttribute(facilityCode);
            locationAttribute.setAttributeType(locationAttributeType);
            location.addAttribute(locationAttribute);
        }
        return location;

    }
}
