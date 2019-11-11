package com.muzima.service;

import android.os.AsyncTask;
import android.os.Build;
import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.User;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.util.MuzimaLogger;
import com.muzima.utils.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

public class MuzimaLoggerService {
    private static String pseudoDeviceId = null;

    public static String getGpsLocation(final android.content.Context context){
        MuzimaGPSLocationService muzimaLocationService = ((MuzimaApplication)context.getApplicationContext()).getMuzimaGPSLocationService();

        HashMap<String, Object> locationDataHashMap = muzimaLocationService.getLastKnownGPS();
        if(locationDataHashMap.containsKey("gps_location")) {
            MuzimaGPSLocation muzimaGPSLocation = ((MuzimaGPSLocation)locationDataHashMap.get("gps_location"));
            try {
                return muzimaGPSLocation.toJsonObject().toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return "{}";
    }

    public static void log(final Context context, final String tag, final String userId, final String gpsLocation, final String details){
        new AsyncTask<Void,Void,Void>(){
            protected Void doInBackground(Void... voids) {
                String deviceId = getPseudoDeviceId();
                System.out.println("Saving log: "+"tag="+tag+" ,userId="+userId+" , gpsLocation= "+gpsLocation
                        +"details="+details+" , deviceId="+deviceId);
                MuzimaLogger.log(context, tag,userId, gpsLocation,details, deviceId);
                return null;
            }
        }.execute();
    }

    public static void log(final android.content.Context applicationContext, final String tag, final String details){
        Context context = ((MuzimaApplication)applicationContext.getApplicationContext()).getMuzimaContext();
        User authenticatedUser = ((MuzimaApplication)applicationContext.getApplicationContext()).getAuthenticatedUser();
        if(authenticatedUser != null) {
            String userId = StringUtils.isEmpty(authenticatedUser.getUsername()) ?
                    authenticatedUser.getSystemId():authenticatedUser.getUsername();
            log(context, tag,userId, getGpsLocation(applicationContext), details);
        } else {
            System.out.println("Could not save logsA");
        }
    }



    private static String getPseudoDeviceId() {
        if(pseudoDeviceId == null){
            pseudoDeviceId = generatePseudoDeviceId();
        }
        return pseudoDeviceId;
    }

    /**
     * Return pseudo unique ID
     * @return ID
     */
    private static String generatePseudoDeviceId() {
        // If all else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their device or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Thanks http://www.pocketmagic.net/?p=1662!
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10)
                + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10)
                + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // https://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their device, there will be a duplicate entry
        String serial = null;
        try {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
            // String needs to be initialized
            serial = "serial"; // some value
        }

        // Thanks @Joe!
        // https://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

}
