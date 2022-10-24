package com.muzima.controller;

import com.muzima.api.model.AppVersion;
import com.muzima.api.service.AppVersionService;

import java.io.IOException;

public class AppVersionController {
    private final AppVersionService appVersionService;

    public AppVersionController(AppVersionService appVersionService) {
        this.appVersionService = appVersionService;
    }

    public AppVersion downloadAppVersion() throws AppVersionController.AppVersionDownloadException {
        try {
            AppVersion appVersion =  appVersionService.downloadAppVersion();
            return  appVersion;
        } catch (IOException e) {
            throw new AppVersionController.AppVersionDownloadException(e);
        }
    }

    public AppVersion getAppVersion() throws AppVersionController.AppVersionFetchException {
        try {
              AppVersion appVersions = appVersionService.getAppVersion();
              AppVersion appVersion = new AppVersion();
              appVersion.setVersion("3.0.9");
              appVersion.setUrl("https://dl.dropboxusercontent.com/s/22nh01nz38oanr5/MD-168-debug-2.8.4-FGH.apk");

              return appVersion;
        } catch (IOException e) {
            throw new AppVersionController.AppVersionFetchException(e);
        }
    }

    public void saveAppVersion(AppVersion appVersion) throws AppVersionController.AppVersionSaveException {
        try {
            appVersionService.saveAppVersion(appVersion);
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
