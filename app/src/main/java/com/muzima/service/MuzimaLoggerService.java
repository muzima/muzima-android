/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.os.Build;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.User;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.util.MuzimaLogger;
import net.minidev.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.UUID;

import static com.muzima.util.Constants.ServerSettings.LOGGING_FEATURE_ENABLED_SETTING;

public class MuzimaLoggerService {
    private static String pseudoDeviceId = null;

    private static Timer timer;

    public static String getAndParseGPSLocationForLogging(final MuzimaApplication muzimaApplication){
        if(isLoggingFeatureEnabled(muzimaApplication)) {
            MuzimaGPSLocationService muzimaLocationService = muzimaApplication.getMuzimaGPSLocationService();

            HashMap<String, Object> locationDataHashMap = muzimaLocationService.getLastKnownGPSLocationAndSettingDetails();
            if (locationDataHashMap.containsKey("gps_location")) {
                MuzimaGPSLocation muzimaGPSLocation = ((MuzimaGPSLocation) locationDataHashMap.get("gps_location"));
                try {
                    return muzimaGPSLocation.toJsonObject().toString();
                } catch (JSONException e) {
                    Log.e("MuzimaLoggerService", "Error while obtaining GPS location", e);
                }
            } else if (locationDataHashMap.containsKey("gps_location_status")) {
                return "{\"gps_location_status\":\"" + locationDataHashMap.get("gps_location_status") + "\"}";
            }
        }
        return "{}";
    }

    public static void log(final MuzimaApplication muzimaApplication, final String tag, final String userId, final String gpsLocation, final String details){
        if(isLoggingFeatureEnabled(muzimaApplication)) {

            new MuzimaAsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {

                }

                protected Void doInBackground(Void... voids) {
                    String deviceId = getPseudoDeviceId();
                    JSONObject timestamp = new JSONObject(){{
                        put("systemTimestamp",System.currentTimeMillis());
                    }};

                    MuzimaLogger.log(muzimaApplication.getMuzimaContext(), tag, userId, gpsLocation, details, deviceId,timestamp.toJSONString());                    return null;
                }

                @Override
                protected void onPostExecute(Void unused) {

                }

                @Override
                protected void onBackgroundError(Exception e) {

                }
            }.execute();
        }
    }

    public static void log(final MuzimaApplication muzimaApplication, final String tag, final String details){
        if(isLoggingFeatureEnabled(muzimaApplication)) {
            User authenticatedUser = muzimaApplication.getAuthenticatedUser();
            if (authenticatedUser != null) {
                String userId = authenticatedUser.getUuid();
                log(muzimaApplication, tag, userId, getAndParseGPSLocationForLogging(muzimaApplication), details);
            } else {
                Log.e("MuzimaLoggerService", "Could not save logs");
            }
        }
    }

    public static void scheduleLogSync(final MuzimaApplication muzimaApplication) {
        if (isLoggingFeatureEnabled(muzimaApplication)) {
            timer = new java.util.Timer();
            timer.schedule(
                new java.util.TimerTask() {

                    @Override
                    public void run() {
                        new MuzimaAsyncTask<Void, Void, Void>() {
                            @Override
                            protected void onPreExecute() {

                            }

                            protected Void doInBackground(Void... voids) {
                                try {
                                    Context context = muzimaApplication.getMuzimaContext();
                                    context.getLogEntryService().syncLogs();
                                } catch (IOException e) {
                                    Log.e("LoggerService", "Error syncing", e);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void unused) {

                            }

                            @Override
                            protected void onBackgroundError(Exception e) {

                            }
                        }.execute();
                    }
                }, 30000, 120000
            );
        }
    }

    public static void stopLogsSync(){
        if(timer != null){
            timer.cancel();
        }
    }

    private static boolean isLoggingFeatureEnabled(final MuzimaApplication muzimaApplication){
        if(muzimaApplication != null) {
            MuzimaSettingController muzimaSettingController = muzimaApplication.getMuzimaSettingController();
            try {
                MuzimaSetting loggingFeatureSetting = muzimaSettingController.getSettingByProperty(LOGGING_FEATURE_ENABLED_SETTING);

                if (loggingFeatureSetting != null) {
                    return loggingFeatureSetting.getValueBoolean();
                }
            } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                Log.e("MuzimaLoggerService", "Could not fetch setting", e);
            }
        }
        return false;
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
        // http://www.pocketmagic.net/?p=1662
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10)
                + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10)
                + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

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

        // https://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

}
