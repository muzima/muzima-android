/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Observation;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.ObservationService;
import com.muzima.service.ConceptParser;
import com.muzima.service.HTMLConceptParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ConceptController {
    private List<Concept> newConcepts = new ArrayList<Concept>();
    private ConceptService conceptService;
    private ObservationService observationService;

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
        HashSet<Concept> result = new HashSet<Concept>();
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
        return new ArrayList<Concept>(result);
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
        HashSet<Concept> concepts = new HashSet<Concept>();
        ConceptParser xmlParserUtils = new ConceptParser();
        HTMLConceptParser htmlParserUtils = new HTMLConceptParser();
        for (FormTemplate formTemplate : formTemplates) {
            List<String> names = new ArrayList<String>();
            if (formTemplate.isHTMLForm()) {
                names = htmlParserUtils.parse(formTemplate.getHtml());
            } else {
                names = xmlParserUtils.parse(formTemplate.getModel());
            }
            concepts.addAll(downloadConceptsByNames(names));
        }
        return new ArrayList<Concept>(concepts);
    }

    public void deleteAllConcepts() throws ConceptDeleteException, ConceptFetchException {
        deleteConcepts(getConcepts());
    }

    public static class ConceptDownloadException extends Throwable {
        public ConceptDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ConceptFetchException extends Throwable {
        public ConceptFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ConceptSaveException extends Throwable {
        public ConceptSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ConceptDeleteException extends Throwable {
        public ConceptDeleteException(Throwable throwable) {
            super(throwable);
        }
    }
    public static class ConceptParseException extends Throwable {
        public ConceptParseException(String message) {
            super(message);
        }
    }
}
