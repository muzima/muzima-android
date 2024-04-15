/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.MinimumSupportedAppVersion;
import com.muzima.api.service.MinimumSupportedAppVersionService;

import java.io.IOException;

public class MinimumSupportedAppVersionController {
    private final MinimumSupportedAppVersionService minimumSupportedAppVersionService;

    public MinimumSupportedAppVersionController(MinimumSupportedAppVersionService minimumSupportedAppVersionService) {
        this.minimumSupportedAppVersionService = minimumSupportedAppVersionService;
    }

    public MinimumSupportedAppVersion downloadMinimumSupportedAppVersion() throws MinimumSupportedAppVersionController.MinimumSupportedAppVersionDownloadException {
        try {
            MinimumSupportedAppVersion minimumSupportedAppVersion =  minimumSupportedAppVersionService.downloadMinimumSupportedAppVersion();
            return  minimumSupportedAppVersion;
        } catch (IOException e) {
            throw new MinimumSupportedAppVersionController.MinimumSupportedAppVersionDownloadException(e);
        }
    }

    public MinimumSupportedAppVersion getMinimumSupportedAppVersion() throws MinimumSupportedAppVersionController.MinimumSupportedAppVersionFetchException {
        try {
            return minimumSupportedAppVersionService.getMinimumSupportedAppVersion();
        } catch (IOException e) {
            throw new MinimumSupportedAppVersionController.MinimumSupportedAppVersionFetchException(e);
        }
    }

    public void saveMinimumSupportedAppVersion(MinimumSupportedAppVersion minimumSupportedAppVersion) throws MinimumSupportedAppVersionController.MinimumSupportedAppVersionSaveException {
        try {
            minimumSupportedAppVersionService.saveMinimumSupportedAppVersion(minimumSupportedAppVersion);
        } catch (IOException e) {
            throw new MinimumSupportedAppVersionController.MinimumSupportedAppVersionSaveException(e);
        }
    }

    public void updateMinimumSupportedAppVersion(MinimumSupportedAppVersion minimumSupportedAppVersion) throws MinimumSupportedAppVersionController.MinimumSupportedAppVersionSaveException {
        try {
            minimumSupportedAppVersionService.updateMinimumSupportedAppVersion(minimumSupportedAppVersion);
        } catch (IOException e) {
            throw new MinimumSupportedAppVersionController.MinimumSupportedAppVersionSaveException(e);
        }
    }

    public static class MinimumSupportedAppVersionFetchException extends Throwable {
        MinimumSupportedAppVersionFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MinimumSupportedAppVersionSaveException extends Throwable {
        MinimumSupportedAppVersionSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MinimumSupportedAppVersionDownloadException extends Throwable {
        MinimumSupportedAppVersionDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}
