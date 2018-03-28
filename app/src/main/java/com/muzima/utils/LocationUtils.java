package com.muzima.utils;

import com.muzima.api.model.Location;
import com.muzima.api.model.LocationAttribute;

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
            facilityCode = LocationUtils.getLocationAttributeValue(location, Constants.Shr.KenyaEmr.LocationAttributeType
                    .MASTER_FACILITY_CODE.name);
            if(StringUtils.isEmpty(facilityCode)){
                facilityCode = LocationUtils.getLocationAttributeValue(location, Constants.Shr.KenyaEmr.LocationAttributeType
                        .MASTER_FACILITY_CODE.uuid);
            }
        }
        return facilityCode;
    }
}
