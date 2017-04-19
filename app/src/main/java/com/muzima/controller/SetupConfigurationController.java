package com.muzima.controller;

import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.service.SetupConfigurationService;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.List;

public class SetupConfigurationController {

    private static final String TAG = "FormController";
    private SetupConfigurationService setupConfigurationService;
    public SetupConfigurationController(SetupConfigurationService setupConfigurationService){
        this.setupConfigurationService = setupConfigurationService;
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
            return setupConfigurationService.downloadSetupConfigurationTemplateByUuid(uuid);
        }catch (IOException e){
            throw new SetupConfigurationDownloadException(e);
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

    public void saveSetupConfigurations(List<SetupConfiguration> setupConfigurations) throws SetupConfigurationSaveException{
        try{
            setupConfigurationService.saveSetupConfigurations(setupConfigurations);
        } catch (IOException e){
            throw new SetupConfigurationSaveException(e);
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
    }
}
