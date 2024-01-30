package com.muzima.controller;

import android.util.Log;

import com.muzima.api.model.HTCPerson;
import com.muzima.api.service.HTCPersonService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HTCPersonController {

    private final HTCPersonService htcPersonService;


    public HTCPersonController(HTCPersonService htcPersonService) {
        this.htcPersonService = htcPersonService;
    }


    public List<HTCPerson> searchPersonOnServer(String name) {
        try {
            return htcPersonService.downloadPersonByName(name);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while searching for person in the server", e);
        }
        return new ArrayList<>();
    }
}
