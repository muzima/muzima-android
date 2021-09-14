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

import android.util.Log;

import com.muzima.api.model.Person;
import com.muzima.api.service.PersonService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.List;

public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    public Person getPersonByUuid(String uuid) throws PersonLoadException {
        try {
            Person person =  personService.getPersonByUuid(uuid);
        return person;
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
