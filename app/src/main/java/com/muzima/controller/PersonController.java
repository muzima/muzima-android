/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.api.service.PersonService;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.DateUtils.parse;

public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    public Person getPersonByUuid(String uuid) throws PersonLoadException {
        try {
            return personService.getPersonByUuid(uuid);
        } catch (IOException e) {
            throw new PersonLoadException(e);
        }
    }

    public List<Person> searchPersonLocally(String term) throws PersonLoadException {
        try {
            return personService.search(term);
        } catch (IOException | ParseException e) {
            throw new PersonLoadException(e);
        }
    }

    public void savePerson(Person person) throws PersonSaveException {
        try {
            personService.savePerson(person);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the person : " + person.getUuid(), e);
            throw new PersonSaveException(e);
        }
    }

    public Person createNewPerson(MuzimaApplication muzimaApplication, String jsonPayload, String personUuid) {
        try {
            JSONObject responseJSON = new JSONObject(jsonPayload);
            JSONObject personJSON = responseJSON.getJSONObject("patient");

            Person person = new Person();
            person.setUuid(personUuid);
            person.setGender(personJSON.getString("patient.sex"));
            List<PersonName> names = new ArrayList<>();
            names.add(getPersonName(personJSON));
            person.setNames(names);
            person.setBirthdate(getBirthDate(personJSON));
            person.setBirthdateEstimated(personJSON.getBoolean("patient.birthdate_estimated"));
            return personService.savePerson(person);
        } catch (Exception e) {
            Log.e("PersonController", e.getMessage(), e);
        }
        return null;
    }

    private PersonName getPersonName(JSONObject jsonObject) throws JSONException {
        PersonName personName = new PersonName();
        personName.setFamilyName(jsonObject.getString("patient.family_name"));
        personName.setGivenName(jsonObject.getString("patient.given_name"));

        String middleNameJSONString = "patient.middle_name";
        String middleName = "";
        if (jsonObject.has(middleNameJSONString))
            middleName = jsonObject.getString(middleNameJSONString);
        personName.setMiddleName(middleName);

        return personName;
    }


    private Date getBirthDate(JSONObject jsonObject) throws JSONException {
        String birthDateAsString = jsonObject.getString("patient.birth_date");
        Date birthDate = null;
        try {
            if (birthDateAsString != null)
                birthDate = parse(birthDateAsString);
        } catch (java.text.ParseException e) {
            Log.e(getClass().getSimpleName(), "Could not parse birth_date", e);
        }
        return birthDate;
    }

    /********************************************************************************************************
     *                               METHODS FOR EXCEPTION HANDLING
     *********************************************************************************************************/

    public static class PersonSaveException extends Throwable {
        public PersonSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class PersonLoadException extends Throwable {
        public PersonLoadException(Throwable e) {
            super(e);
        }
    }
}
