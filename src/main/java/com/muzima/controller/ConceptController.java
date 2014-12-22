/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.ObservationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ConceptController {
    public static List<Concept> newConcepts = new ArrayList<Concept>();
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
                if (!next.containsNameIgnoreLowerCase(name)) {
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
}
