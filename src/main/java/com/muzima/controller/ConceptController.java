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
import com.muzima.api.service.ConceptService;

import java.io.IOException;
import java.util.List;

/**
 * TODO: Write brief description about the class here.
 */
public class ConceptController {
    private ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    public List<Concept> downloadConceptsByName(String name) throws ConceptDownloadException {
        try {
            return conceptService.downloadConceptsByName(name);
        } catch (IOException e) {
            throw new ConceptDownloadException(e);
        }
    }

    public List<Concept> getConceptByName(String name) throws ConceptFetchException {
        try {
            return conceptService.getConceptsByName(name);
        } catch (IOException e) {
            throw new ConceptFetchException(e);
        }
    }

    public Concept getConceptByUuid(String uuid) throws ConceptFetchException {
        try {
            return conceptService.getConceptByUuid(uuid);
        } catch (IOException e) {
            throw new ConceptFetchException(e);
        }
    }

    public void saveConcept(Concept concept) throws ConceptSaveException {
        try {
            conceptService.saveConcept(concept);
        } catch (IOException e) {
            throw new ConceptSaveException(e);
        }
    }

    public void deleteConcept(Concept concept) throws ConceptDeleteException {
        try {
            conceptService.deleteConcept(concept);
        } catch (IOException e) {
            throw new ConceptDeleteException(e);
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

    public static class ConceptReplaceException extends Throwable {
        public ConceptReplaceException(Throwable throwable) {
            super(throwable);
        }
    }
}
