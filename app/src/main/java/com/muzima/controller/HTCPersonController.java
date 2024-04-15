package com.muzima.controller;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.service.HTCPersonService;
import com.muzima.api.service.MuzimaHtcFormService;
import com.muzima.service.MuzimaLoggerService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.muzima.utils.Constants;

public class HTCPersonController {
    private final HTCPersonService htcPersonService;
    private final MuzimaHtcFormService htcFormService;

    private MuzimaApplication muzimaApplication;
    public HTCPersonController(HTCPersonService htcPersonService, MuzimaHtcFormService htcFormService, MuzimaApplication muzimaApplication) {
        this.htcPersonService = htcPersonService;
        this.htcFormService = htcFormService;
        this.muzimaApplication = muzimaApplication;
    }
    public HTCPerson getHTCPerson(String uuid) {
        try {
        HTCPerson htcPerson = htcPersonService.getHTCPersonByUuid(uuid);
        return htcPerson;
            } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while getting htc person with uuid : " + uuid, e);
            }
        return null;
    }
    public List<HTCPerson> searchPersonOnServer(String name) {
        try {
            List<HTCPerson> htcsPeople = htcPersonService.downloadPersonByName(name);
            return htcsPeople;
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for person in the server", e);
        }
        return new ArrayList<>();
    }
    public void saveHTCPerson(HTCPerson htcPerson) {
        try {
            htcPerson.setSyncStatus(Constants.STATUS_COMPLETE);
            htcPersonService.saveHTCPerson(htcPerson);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for person in the server", e);
        }
    }
    public List<HTCPerson> searchPersons(String parameter) {
        try {
            List<HTCPerson> htcsPeople = htcPersonService.search(parameter, false);
            return htcsPeople;
        } catch (IOException | ParseException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for person in the server", e);
        }
        return new ArrayList<>();
    }

    public List<HTCPerson> getLatestHTCPersons() {
        try {
            List<HTCPerson> htcsPeople = htcPersonService.getAllHTCPersons();
            return htcsPeople;
        } catch (IOException | ParseException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for person in the server", e);
        }
        return new ArrayList<>();
    }
    public void updateHTCPerson(HTCPerson htcPerson) {
        try {
            htcPersonService.updateHTCPerson(htcPerson);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while updating person " + htcPerson.getUuid(), e);
        }
    }
    public List<HTCPerson> downloadHtcPersonsOfProvider(String providerUuid) {
        try {
            return htcPersonService.downloadHtcPersonsOfProvider(providerUuid);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for htc person on the server", e);
        }
        return new ArrayList<>();
    }

    public void saveHtcPersons(List<HTCPerson> htcPersonList) {
        try {
            htcPersonService.saveHTCPersons(htcPersonList);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving htc person", e);
        }
    }

    public boolean uploadAllPendingHtcData() throws UploadHtcDataException {
        boolean result = false;
        try {
        List<HTCPerson> htcPersonList = htcPersonService.getBySyncStatus(Constants.STATUS_COMPLETE);
            for (HTCPerson person : htcPersonList) {
                person.setHtcForm(htcFormService.getHTCFormByHTCPersonUuid(person.getUuid()));
                if (htcPersonService.syncHtcData(person)) {
                    person.setSyncStatus(Constants.STATUS_UPLOADED);
                    htcPersonService.updateHTCPerson(person);
                    result = true;
                    MuzimaLoggerService.log(muzimaApplication, "SYNCED_HTC_DATA", "{\"htcPersonUuid\":\"" + person.getUuid() + "\"}");
                } else {
                    result = false;
                }
            }
        } catch (IOException | ParseException e) {
            throw new UploadHtcDataException(e);
        }
        return  result;

    }

    public void deleteHtcPersonPendingDeletion() {
    }

    public static class HTCPersonDownloadException extends Throwable {
        HTCPersonDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class HTCPersonSaveException extends Throwable {
        HTCPersonSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class HTCPersonFetchException extends Throwable {
        HTCPersonFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class HTCPersonDeleteException extends Throwable {
        HTCPersonDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ParseHTCPersonException extends Throwable {
        public ParseHTCPersonException(Throwable e) {
            super(e);
        }
        public ParseHTCPersonException(String message) {
            super(message);
        }
    }

    public static class UploadHtcDataException extends Throwable {
        UploadHtcDataException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class HtcFetchException extends Throwable {
        public HtcFetchException(Throwable throwable) {
            super(throwable);
        }
    }
}
