/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Observation;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.ObservationService;
import com.muzima.util.JsonUtils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class ConceptController {
    private List<Concept> newConcepts = new ArrayList<>();
    private final ConceptService conceptService;
    private final ObservationService observationService;

    public ConceptController(ConceptService conceptService, ObservationService observationService) {
        this.observationService = observationService;
        this.conceptService = conceptService;
    }

    public List<Concept> downloadConceptsByNamePrefix(String name) throws ConceptDownloadException {
        try {
            return conceptService.downloadConceptsByName(name);
        } catch (IOException e) {
            throw new ConceptDownloadException(e);
        }
    }

    private Concept downloadConceptByUuid(String uuid) throws ConceptDownloadException {
        try {
            return conceptService.downloadConceptByUuid(uuid);
        } catch (IOException e) {
            throw new ConceptDownloadException(e);
        }
    }

    public void deleteConcept(Concept concept) throws ConceptDeleteException {
        try {
            conceptService.deleteConcept(concept);
            List<Observation> observations = observationService.getObservations(concept);
            observationService.deleteObservations(observations);
        } catch (IOException e) {
            throw new ConceptDeleteException(e);
        }
    }

    public void deleteConcepts(List<Concept> concepts) throws ConceptDeleteException {
        try {
            conceptService.deleteConcepts(concepts);
            for(Concept concept : concepts){
                List<Observation> observations = observationService.getObservations(concept);
                observationService.deleteObservations(observations);
            }
        } catch (IOException e) {
            throw new ConceptDeleteException(e);
        }
    }

    public void saveConcepts(List<Concept> concepts) throws ConceptSaveException {
        try {
            conceptService.saveConcepts(concepts);
        } catch (IOException e) {
            throw new ConceptSaveException(e);
        }
    }

    public Concept getConceptByName(String name) throws ConceptFetchException {
        try {
            List<Concept> concepts = conceptService.getConceptsByName(name);
            for (Concept concept : concepts) {
                if (concept.getName().equals(name)) {
                    return concept;
                }
            }
        } catch (IOException e) {
            throw new ConceptFetchException(e);
        }
        return null;
    }

    public List<Concept> downloadConceptsByNames(List<String> names) throws ConceptDownloadException {
        HashSet<Concept> result = new HashSet<>();
        for (String name : names) {
            List<Concept> concepts = downloadConceptsByNamePrefix(name);
            Iterator<Concept> iterator = concepts.iterator();
            while (iterator.hasNext()) {
                Concept next = iterator.next();
                if (next == null || !next.containsNameIgnoreLowerCase(name)) {
                    iterator.remove();
                }
            }
            result.addAll(concepts);
        }
        return new ArrayList<>(result);
    }

    public List<Concept> downloadConceptsByUuid(String[] uuids) throws ConceptDownloadException {
        HashSet<Concept> result = new HashSet<>();
        for (String uuid : uuids) {
            Concept concept = downloadConceptByUuid(uuid);
            if(concept != null) result.add(concept);
        }
        return new ArrayList<>(result);
    }

    public List<Concept> getConcepts() throws ConceptFetchException {
        try {
            List<Concept> allConcepts = conceptService.getAllConcepts();
            Collections.sort(allConcepts);
            return allConcepts;
        } catch (IOException e) {
            throw new ConceptFetchException(e);
        }
    }

    public void newConcepts(List<Concept> concepts) throws ConceptFetchException {
        newConcepts = concepts;
        List<Concept> savedConcepts = getConcepts();
        newConcepts.removeAll(savedConcepts);
    }

    public List<Concept> newConcepts() {
        return newConcepts;
    }

    public List<Concept> getRelatedConcepts(List<FormTemplate> formTemplates) throws ConceptDownloadException {
        HashSet<String> uuids = new HashSet<>();
        for (FormTemplate formTemplate : formTemplates) {
            Object uuidsObject = JsonUtils.readAsObject(formTemplate.getMetaJson(),"$['concepts']");

            if(uuidsObject instanceof JSONArray){
                JSONArray uuidsArray = (JSONArray)uuidsObject;
                for(Object obj : uuidsArray    ){
                    JSONObject conceptObj = (JSONObject)obj;
                    uuids.add((String)conceptObj.get("uuid"));
                }
            } else if(uuidsObject instanceof LinkedHashMap){
                LinkedHashMap obj = (LinkedHashMap) uuidsObject;
                if(obj.containsKey("uuid")){
                    uuids.add((String)obj.get("uuid"));
                }
                uuids.add(uuidsObject.toString());
            }
        }
        return downloadConceptsByUuid(uuids.toArray(new String[uuids.size()]));
    }

    public void deleteAllConcepts() throws ConceptDeleteException, ConceptFetchException {
        deleteConcepts(getConcepts());
    }

    public static class ConceptDownloadException extends Throwable {
        ConceptDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ConceptFetchException extends Throwable {
        ConceptFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ConceptSaveException extends Throwable {
        ConceptSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ConceptDeleteException extends Throwable {
        ConceptDeleteException(Throwable throwable) {
            super(throwable);
        }
    }
    public static class ConceptParseException extends Throwable {
        public ConceptParseException(String message) {
            super(message);
        }
    }
}
