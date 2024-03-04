package com.muzima.controller;

import android.util.Log;

import com.muzima.api.model.HTCPerson;
import com.muzima.api.service.HTCPersonService;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HTCPersonController {
    private final HTCPersonService htcPersonService;
    public HTCPersonController(HTCPersonService htcPersonService) {
        this.htcPersonService = htcPersonService;
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
            Log.e(getClass().getSimpleName(), "Error while updating person "+htcPerson.getUuid(), e);
        }
    }
}
