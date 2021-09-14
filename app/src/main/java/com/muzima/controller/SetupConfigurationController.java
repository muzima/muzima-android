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

import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.SetupConfigurationService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_SETUP_CONFIGURATIONS;

public class SetupConfigurationController {

    private final SetupConfigurationService setupConfigurationService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public SetupConfigurationController(SetupConfigurationService setupConfigurationService,
                                        LastSyncTimeService lastSyncTimeService, SntpService sntpService){
        this.setupConfigurationService = setupConfigurationService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<SetupConfiguration> downloadAllSetupConfigurations() throws SetupConfigurationDownloadException{
        try {
            return setupConfigurationService.downloadSetupConfigurationsByName(StringUtils.EMPTY);
        }catch (IOException e){
            throw new SetupConfigurationDownloadException(e);
        }
    }

    public List<SetupConfiguration> getAllSetupConfigurations() throws SetupConfigurationDownloadException{
        try{
            return setupConfigurationService.getAllSetupConfigurations();
        }catch (IOException e){
            throw new SetupConfigurationDownloadException(e);
        }
    }

    public SetupConfigurationTemplate downloadSetupConfigurationTemplate(String uuid) throws SetupConfigurationDownloadException{
        try{
            SetupConfigurationTemplate template = setupConfigurationService.downloadSetupConfigurationTemplateByUuid(uuid);

            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_SETUP_CONFIGURATIONS, sntpService.getTimePerDeviceTimeZone(), uuid);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);

            return template;
        }catch (IOException e){
            throw new SetupConfigurationDownloadException(e);
        }
    }

    public SetupConfigurationTemplate downloadUpdatedSetupConfigurationTemplate(String uuid) throws SetupConfigurationDownloadException{
        try{
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_SETUP_CONFIGURATIONS, uuid);
            SetupConfigurationTemplate template = setupConfigurationService.downloadUpdatedSetupConfigurationTemplateByUuid(uuid,lastSyncTime);

            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_SETUP_CONFIGURATIONS, sntpService.getTimePerDeviceTimeZone(), uuid);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);

            return template;
        }catch (IOException e){
            throw new SetupConfigurationDownloadException(e);
        }
    }

    public List<SetupConfigurationTemplate> getSetupConfigurationTemplates() throws SetupConfigurationFetchException{
        try{
            return setupConfigurationService.getSetupConfigurationTemplates();
        }catch (IOException e){
            throw new SetupConfigurationFetchException(e);
        }
    }

    public SetupConfigurationTemplate getSetupConfigurationTemplate(final String uuid) throws SetupConfigurationFetchException{
        try{
            return setupConfigurationService.getSetupConfigurationTemplate(uuid);
        }catch (IOException e){
            throw new SetupConfigurationFetchException(e);
        }
    }

    public void saveSetupConfigurationTemplate(SetupConfigurationTemplate setupConfigurationTemplate) throws SetupConfigurationSaveException{
        try{
            setupConfigurationService.saveSetupConfigurationTemplate(setupConfigurationTemplate);
        }catch (IOException e){
            throw new SetupConfigurationSaveException(e);
        }
    }

    public void updateSetupConfigurationTemplate(SetupConfigurationTemplate setupConfigurationTemplate) throws SetupConfigurationSaveException{
        try{
            setupConfigurationService.updateSetupConfigurationTemplate(setupConfigurationTemplate);
        }catch (IOException e){
            throw new SetupConfigurationSaveException(e);
        }
    }

    public void saveSetupConfigurations(List<SetupConfiguration> setupConfigurations) throws SetupConfigurationSaveException{
        try{
            setupConfigurationService.saveSetupConfigurations(setupConfigurations);
        } catch (IOException e){
            throw new SetupConfigurationSaveException(e);
        }
    }

    public SetupConfigurationTemplate getActiveSetupConfigurationTemplate() throws SetupConfigurationFetchException{
        try{
            List<SetupConfigurationTemplate> setupConfigurationTemplates = setupConfigurationService.getSetupConfigurationTemplates();
            if(setupConfigurationTemplates.size() == 1){
                return setupConfigurationTemplates.get(0);
            } else if(setupConfigurationTemplates.size() > 1){
                //For now, the app supports only one setup config template
                //Logic should be updated here in case multiple configs are supported in future
                throw new SetupConfigurationFetchException("Could not uniquely identify active setup config templates");
            } else {
                throw new SetupConfigurationFetchException("Could not find any setup config templates");
            }
        } catch (IOException e){
            throw new SetupConfigurationFetchException("Could not retrieve setup config templates",e);
        }
    }

    public static class SetupConfigurationDownloadException  extends Throwable {
        SetupConfigurationDownloadException(Throwable throwable){
            super(throwable);
        }
    }

    public static class SetupConfigurationSaveException  extends Throwable {
        SetupConfigurationSaveException(Throwable throwable){
            super(throwable);
        }
    }

    public static class SetupConfigurationFetchException  extends Throwable {
        SetupConfigurationFetchException(Throwable throwable){
            super(throwable);
        }

        SetupConfigurationFetchException(String message,Throwable throwable){
            super(message,throwable);
        }

        SetupConfigurationFetchException(String message){
            super(message);
        }
    }
}
