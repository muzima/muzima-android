package com.muzima.controller;

import com.muzima.api.model.MuzimaCoreModuleVersion;
import com.muzima.api.service.MuzimaCoreModuleVersionService;

import java.io.IOException;

public class MuzimaCoreModuleVersionController {
    private final MuzimaCoreModuleVersionService muzimaCoreModuleVersionService;

    public MuzimaCoreModuleVersionController(MuzimaCoreModuleVersionService muzimaCoreModuleVersionService) {
        this.muzimaCoreModuleVersionService = muzimaCoreModuleVersionService;
    }

    public MuzimaCoreModuleVersion downloadMuzimaCoreModuleVersion() throws MuzimaCoreModuleVersionDownloadException {
        try {
            MuzimaCoreModuleVersion muzimaCoreModuleVersion =  muzimaCoreModuleVersionService.downloadMuzimaCoreModuleVersion();
            return  muzimaCoreModuleVersion;
        } catch (IOException e) {
            throw new MuzimaCoreModuleVersionDownloadException(e);
        }
    }

    public MuzimaCoreModuleVersion getMuzimaCoreModuleVersion() throws MuzimaCoreModuleVersionFetchException{
        try {
            return muzimaCoreModuleVersionService.getMuzimaCoreModuleVersion();
        } catch (IOException e) {
            throw new MuzimaCoreModuleVersionFetchException(e);
        }
    }

    public void saveMuzimaCoreModuleVersion(MuzimaCoreModuleVersion muzimaCoreModuleVersion) throws MuzimaCoreModuleVersionSaveException{
        try {
            muzimaCoreModuleVersionService.saveMuzimaCoreModuleVersion(muzimaCoreModuleVersion);
        } catch (IOException e) {
            throw new MuzimaCoreModuleVersionSaveException(e);
        }
    }

    public void updateMuzimaCoreModuleVersion(MuzimaCoreModuleVersion muzimaCoreModuleVersion) throws MuzimaCoreModuleVersionSaveException{
        try {
            muzimaCoreModuleVersionService.updateMuzimaCoreModuleVersion(muzimaCoreModuleVersion);
        } catch (IOException e) {
            throw new MuzimaCoreModuleVersionSaveException(e);
        }
    }

    public static class MuzimaCoreModuleVersionFetchException extends Throwable {
        MuzimaCoreModuleVersionFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MuzimaCoreModuleVersionSaveException extends Throwable {
        MuzimaCoreModuleVersionSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MuzimaCoreModuleVersionDownloadException extends Throwable {
        MuzimaCoreModuleVersionDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

}
