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

    public void saveOrUpdateAppUsageLogs(List<AppUsageLogs> appUsageLogs) throws IOException, ParseException {
        appUsageLogsService.saveOrUpdateAppUsageLogs(appUsageLogs);
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
