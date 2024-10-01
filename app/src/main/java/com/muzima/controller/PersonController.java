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

import com.muzima.api.model.CohortData;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonTag;
import com.muzima.api.service.PersonService;
import com.muzima.api.service.PersonTagService;
import com.muzima.utils.CustomColor;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonController {

    private final PersonService personService;
    private final PersonTagService personTagService;
    private final Map<String, Integer> tagColors;

    public PersonController(PersonService personService, PersonTagService personTagService) {
        this.personService = personService;
        this.personTagService = personTagService;
        tagColors = new HashMap<>();
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

    public void savePersons(List<Person> persons) throws PersonSaveException {
        try {
            personService.saveOrUpdatePersons(persons);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the persons", e);
            throw new PersonSaveException(e);
        }
    }

    public List<Person> getAllPersons() throws PersonLoadException {
        try {
            return personService.getAllPersons();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching persons : ", e);
            throw new PersonLoadException(e);
        }
    }

    public void deletePersons(List<Person> persons) throws PersonDeleteException {
        try {
            personService.deletePersons(persons);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while deleting persons : ", e);
            throw new PersonDeleteException(e);
        }
    }

    public void updatePerson(Person person) throws IOException {
        personService.updatePerson(person);
    }

    public List<PersonTag> getAllPersonTags() throws PersonLoadException, IOException {
        return personTagService.getAllPersonTags();
    }

    public void savePersonTags(PersonTag personTag) throws IOException {
         personTagService.savePersonTag(personTag);
    }

    public int getTagColor(String uuid) {
        if (!tagColors.containsKey(uuid)) {
            tagColors.put(uuid, CustomColor.getRandomColor());
        }
        return tagColors.get(uuid);
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

    public static class PersonDeleteException extends Throwable {
        public PersonDeleteException(Throwable throwable) {
            super(throwable);
        }
    }
}
