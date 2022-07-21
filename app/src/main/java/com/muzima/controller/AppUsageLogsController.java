package com.muzima.controller;

import static com.muzima.api.model.APIName.APP_USAGE_LOGS;
import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Provider;
import com.muzima.api.service.AppUsageLogsService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppUsageLogsController {
    private final AppUsageLogsService appUsageLogsService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public AppUsageLogsController(AppUsageLogsService appUsageLogsService, LastSyncTimeService lastSyncTimeService, SntpService sntpService){
        this.appUsageLogsService = appUsageLogsService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
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
        Date lastSyncTimeForLogs = lastSyncTimeService.getLastSyncTimeFor(APP_USAGE_LOGS);
        LastSyncTime lastSyncTime = new LastSyncTime(APP_USAGE_LOGS, sntpService.getTimePerDeviceTimeZone());
        try {
            if(appUsageLogs.size() > 0){
                for(AppUsageLogs appUsageLog : appUsageLogs){
                   if(lastSyncTimeForLogs != null) {
                        if (lastSyncTimeForLogs.before(appUsageLog.getUpdateDatetime())) {
                            appUsageLogsService.syncAppUsageLogs(appUsageLog);
                        }
                    }else{
                        appUsageLogsService.syncAppUsageLogs(appUsageLog);
                    }
                }
                lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),"Encounter an IO exception",e);
        }

        return true;
    }
}
