/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.DerivedConcept;
import com.muzima.api.service.DerivedConceptService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DerivedConceptController {
    private DerivedConceptService derivedConceptService;

    public DerivedConceptController(DerivedConceptService derivedConceptService){
        this.derivedConceptService = derivedConceptService;
    }

    public List<DerivedConcept> downloadDerivedConceptsByUuid(String[] uuids) throws DerivedConceptDownloadException {
        HashSet<DerivedConcept> result = new HashSet<>();
        for (String uuid : uuids) {
            DerivedConcept derivedConcept = null;
            try {
                derivedConcept = derivedConceptService.downloadDerivedConceptByUuid(uuid);
                if(derivedConcept != null)
                    result.add(derivedConcept);
            } catch (IOException e) {
                throw new DerivedConceptDownloadException(e);
            }

        }
        return new ArrayList<>(result);
    }

    public void saveDerivedConcepts(List<DerivedConcept> derivedConcepts) throws DerivedConceptSaveException {
        try {
            derivedConceptService.saveDerivedConcepts(derivedConcepts);
        } catch (IOException e) {
            throw new DerivedConceptSaveException(e);
        }
    }

    public List<DerivedConcept> getDerivedConcepts() throws DerivedConceptFetchException {
        try {
            return derivedConceptService.getDerivedConcepts();
        } catch (IOException e) {
            throw new DerivedConceptFetchException(e);
        }
    }

    public DerivedConcept getDerivedConceptByUuid(String uuid) throws DerivedConceptFetchException {
        try {
            return derivedConceptService.getDerivedConceptByUuid(uuid);
        } catch (IOException e) {
            throw new DerivedConceptFetchException(e);
        }
    }

    public DerivedConcept getDerivedConceptById(int id) throws DerivedConceptFetchException {
        try {
            return derivedConceptService.getDerivedConceptById(id);
        } catch (IOException e) {
            throw new DerivedConceptFetchException(e);
        }
    }

    public void deleteDerivedConcepts(List<DerivedConcept> derivedConcepts) throws DerivedConceptDeleteException {
        try {
            derivedConceptService.deleteDerivedConcepts(derivedConcepts);
        } catch (IOException e) {
            throw new DerivedConceptDeleteException(e);
        }
    }

    public static class DerivedConceptDownloadException extends Throwable {
        DerivedConceptDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedConceptSaveException extends Throwable {
        DerivedConceptSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedConceptFetchException extends Throwable {
        DerivedConceptFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedConceptDeleteException extends Throwable {
        DerivedConceptDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedConceptParseException extends Throwable {
        public DerivedConceptParseException(String message) {
            super(message);
        }
    }
}
