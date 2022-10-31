package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_LATEST_APP_VERSION;

import com.muzima.api.model.AppVersion;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.AppVersionService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AppVersionController {
    private final AppVersionService appVersionService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public AppVersionController(AppVersionService appVersionService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.appVersionService = appVersionService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<AppVersion> downloadAppVersion() throws AppVersionController.AppVersionDownloadException {
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_LATEST_APP_VERSION);
            Date lastSyncDate = null;
            if (lastSyncTime != null) {
                lastSyncDate = lastSyncTime.getLastSyncDate();
            }
            List<AppVersion> appVersion =  appVersionService.downloadAppVersion(lastSyncDate);
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_LATEST_APP_VERSION, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return  appVersion;
        } catch (IOException e) {
            throw new AppVersionController.AppVersionDownloadException(e);
        }
    }

    public AppVersion getAppVersion() throws AppVersionController.AppVersionFetchException {
        try {
              AppVersion appVersion = appVersionService.getAppVersion();
              return appVersion;
        } catch (IOException e) {
            throw new AppVersionController.AppVersionFetchException(e);
        }
    }

    public void saveAppVersion(List<AppVersion> appVersions) throws AppVersionController.AppVersionSaveException {
        try {
            appVersionService.saveAppVersion(appVersions);
        } catch (IOException e) {
            throw new AppVersionController.AppVersionSaveException(e);
        }
    }

    public void updateAppVersion(AppVersion appVersion) throws AppVersionController.AppVersionSaveException {
        try {
            appVersionService.updateAppVersion(appVersion);
        } catch (IOException e) {
            throw new AppVersionController.AppVersionSaveException(e);
        }
    }

    public static class AppVersionFetchException extends Throwable {
        AppVersionFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class AppVersionSaveException extends Throwable {
        AppVersionSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class AppVersionDownloadException extends Throwable {
        AppVersionDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}
