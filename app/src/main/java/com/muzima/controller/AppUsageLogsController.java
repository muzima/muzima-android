/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;

import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.service.AppUsageLogsService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.List;

public class AppUsageLogsController {
    private final AppUsageLogsService appUsageLogsService;

    public AppUsageLogsController(AppUsageLogsService appUsageLogsService){
        this.appUsageLogsService = appUsageLogsService;
    }

    public void saveOrUpdateAppUsageLog(AppUsageLogs appUsageLog) throws IOException, ParseException {
        appUsageLogsService.saveOrUpdateAppUsageLog(appUsageLog);
    }

    public AppUsageLogs getAppUsageLogByKey(String key) throws IOException, ParseException {
       return appUsageLogsService.getAppUsageLogByKey(key);
    }

    public AppUsageLogs getAppUsageLogByKeyAndUserName(String key, String username) throws IOException, ParseException {
       return appUsageLogsService.getAppUsageLogByKeyAndUserName(key,username);
    }

    public List<AppUsageLogs> getAllAppUsageLogs() throws IOException {
        return appUsageLogsService.getAllAppUsageLogs();
    }

    public boolean syncAppUsageLogs(List<AppUsageLogs> appUsageLogs) throws IOException {
        try {
            if(appUsageLogs.size() > 0){
                for(AppUsageLogs appUsageLog : appUsageLogs){
                   if(!appUsageLog.isLogSynced()) {
                       boolean isSyncSuccessful = appUsageLogsService.syncAppUsageLogs(appUsageLog);
                       if(isSyncSuccessful){
                           appUsageLog.setLogSynced(true);
                           appUsageLogsService.saveOrUpdateAppUsageLog(appUsageLog);
                       }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),"Encounter an IO exception ",e);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Encountered a Parse Exception ",e);
        }

        return true;
    }
}
