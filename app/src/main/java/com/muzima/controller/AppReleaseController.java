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

import static com.muzima.api.model.APIName.DOWNLOAD_LATEST_APP_RELEASES;

import com.muzima.api.model.AppRelease;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.AppReleaseService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class AppReleaseController {
    private final AppReleaseService appReleaseService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public AppReleaseController(AppReleaseService appReleaseService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.appReleaseService = appReleaseService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<AppRelease> downloadAppRelease() throws AppReleaseController.AppReleaseDownloadException {
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_LATEST_APP_RELEASES);
            Date lastSyncDate = null;
            if (lastSyncTime != null) {
                lastSyncDate = lastSyncTime.getLastSyncDate();
            }
            List<AppRelease> appRelease =  appReleaseService.downloadAppRelease(lastSyncDate);
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_LATEST_APP_RELEASES, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return  appRelease;
        } catch (IOException e) {
            throw new AppReleaseController.AppReleaseDownloadException(e);
        }
    }

    public AppRelease getAppRelease() throws AppReleaseController.AppReleaseFetchException {
        try {

            AppRelease appRelease = appReleaseService.getAppRelease();
            return appRelease;
        } catch (IOException e) {
            throw new AppReleaseController.AppReleaseFetchException(e);
        }
    }

    public void saveAppRelease(List<AppRelease> appReleases) throws AppReleaseController.AppReleaseSaveException {
        try {
            appReleaseService.saveAppRelease(appReleases);
        } catch (IOException e) {
            throw new AppReleaseController.AppReleaseSaveException(e);
        }
    }

    public static class AppReleaseFetchException extends Throwable {
        AppReleaseFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class AppReleaseSaveException extends Throwable {
        AppReleaseSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class AppReleaseDownloadException extends Throwable {
        AppReleaseDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}
