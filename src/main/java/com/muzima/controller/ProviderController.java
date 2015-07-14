package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Provider;
import com.muzima.api.service.ProviderService;
import com.muzima.service.HTMLProviderParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class ProviderController {


    public static final String TAG = "ProviderController";
    private ProviderService providerService;
    public List<Provider> newProviders = new ArrayList<Provider>();

    public ProviderController(ProviderService providerService){
        this.providerService = providerService;
    }

    public List<Provider> downloadProviderFromServerByName(String name) throws ProviderDownloadException {
        try {
            return providerService.downloadProvidersByName(name);
        } catch (IOException e) {
            Log.e(TAG, "Error while searching for patients in the server", e);
            throw new ProviderDownloadException(e);
        }
    }

    public List<Provider> downloadProvidersFromServerByName(List<String> names) throws ProviderDownloadException {
        HashSet<Provider> result = new HashSet<Provider>();
        for (String name : names) {
            List<Provider> providers = downloadProviderFromServerByName(name);
            result.addAll(providers);
        }
        return new ArrayList<Provider>(result);
    }

    public Provider downloadProviderFromServerByUuid(String uuid) throws ProviderDownloadException {
        try {
            return providerService.downloadProviderByUuid(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Error while searching for patients in the server", e);
            throw new ProviderDownloadException(e);
        }
    }

    public List<Provider> getAllProviders() throws ProviderLoadException {
        try {
            return providerService.getAllProviders();
        } catch (IOException e) {
            throw new ProviderLoadException(e);
        }
    }

    public void saveProvider(Provider provider) throws ProviderSaveException {
        try {
            providerService.saveProvider(provider);
        } catch (IOException e) {
            Log.e(TAG, "Error while saving the provider : " + provider.getUuid(), e);
            throw new ProviderSaveException(e);
        }
    }

    public void saveProviders(List<Provider> providers) throws ProviderSaveException {
        try {
            providerService.saveProviders(providers);
        } catch (IOException e) {
            throw new ProviderSaveException(e);
        }
    }

    public Provider getProviderByUuid(String uuid) throws ProviderLoadException {
        try {
            return providerService.getProviderByUuid(uuid);
        } catch (IOException e) {
            throw new ProviderLoadException(e);
        }
    }

    public Provider getProviderByName(String name) throws ProviderLoadException  {
        try {
            List<Provider> providers = providerService.getProvidersByName(name);
            for (Provider provider : providers) {
                if (provider.getName().equals(name)) {
                    return provider;
                }
            }
        } catch (IOException e) {
            throw new ProviderLoadException(e);
        }
        catch (org.apache.lucene.queryParser.ParseException e) {
            throw new ProviderLoadException(e);
        }
        return null;
    }

    public void deleteProvider(Provider provider) throws ProviderDeleteException {
        try {
            providerService.deleteProvider(provider);
        } catch (IOException e) {
            throw new ProviderDeleteException(e);
        }
    }

    public void deleteProviders(List<Provider> providers) throws ProviderDeleteException {
        try {
            providerService.deleteProviders(providers);
        } catch (IOException e) {
            throw new ProviderDeleteException(e);
        }

    }

    public Provider downloadProviderBySystemId(String systemId) throws ProviderLoadException {
        try {
            return providerService.downloadProvidersBySystemId(systemId);
        } catch (IOException e) {
            throw new ProviderLoadException(e);
        }
    }

    public Provider getProviderBySystemId(String systemId) {
        try {
            return providerService.getProviderBySystemId(systemId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Provider> getRelatedProviders(List<FormTemplate> formTemplates, String systemId) throws ProviderDownloadException, ProviderLoadException {
        HashSet<Provider> providers = new HashSet<Provider>();
        HTMLProviderParser htmlParserUtils = new HTMLProviderParser();
        for (FormTemplate formTemplate : formTemplates) {
            List<String> names = new ArrayList<String>();
            if (formTemplate.isHTMLForm()) {
                names = htmlParserUtils.parse(formTemplate.getHtml());
            } else {
                // names = xmlParserUtils.parse(formTemplate.getModel());
            }
            providers.addAll(downloadProvidersFromServerByName(names));
        }

        //Download the provider data in to local repo for logged in user
        Provider loggedInProvider = downloadProviderBySystemId(systemId);
        if(loggedInProvider != null){
            providers.add(downloadProviderBySystemId(systemId));
        }
        return new ArrayList<Provider>(providers);
    }

    public void newProviders(List<Provider> providers) throws ProviderLoadException {
        newProviders = providers;
        List<Provider> savedProviders = getAllProviders();
        newProviders.removeAll(savedProviders);
    }

    public List<Provider> newProviders() {
        return newProviders;
    }

    public void deleteAllProviders() throws ProviderDeleteException, ProviderLoadException {
        deleteProviders(getAllProviders());
    }

    public static class ProviderSaveException extends Throwable {
        public ProviderSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ProviderDownloadException extends Throwable {
        public ProviderDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ProviderLoadException extends Throwable {
        public ProviderLoadException(Throwable e) {
            super(e);
        }
    }

    public static class ProviderDeleteException extends Throwable {
        public ProviderDeleteException(Throwable e) {
            super(e);
        }
    }


}
