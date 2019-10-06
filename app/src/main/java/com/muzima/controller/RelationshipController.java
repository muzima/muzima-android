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
import com.muzima.api.model.Patient;
import com.muzima.api.model.Relationship;
import com.muzima.api.service.RelationshipService;

import java.io.IOException;
import java.util.List;

public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    public List<Relationship> downloadRelationshipsForPerson(String patientUuid) throws RetrieveRelationshipException {
        try {
            return relationshipService.downloadRelationshipsForPerson(patientUuid);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while downloading Patient Relationships for patient with UUID : " + patientUuid + " from server", e);
            throw new RetrieveRelationshipException(e);
        }
    }


    public void saveRelationships(List<Relationship> relationships) throws SaveRelationshipException {
        try {
            relationshipService.saveRelationships(relationships);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the relationships list", e);
            throw new SaveRelationshipException(e);
        }
    }

//    public int getEncountersCountByPatient(String patientUuid) throws IOException {
//        return relationshipService.countEncountersByPatientUuid(patientUuid);
//    }

    public List<Relationship>  getRelationshipsForPerson(String personUuid) throws RetrieveRelationshipException{
        try {
            return relationshipService.getRelationshipsForPerson(personUuid);
        } catch (IOException e) {
            throw new RetrieveRelationshipException(e);
        }
    }

    public static class RetrieveRelationshipException extends Throwable {
        RetrieveRelationshipException(IOException e) {
            super(e);
        }
    }

//    public class ReplaceEncounterException extends Throwable {
//        ReplaceEncounterException(IOException e) {
//            super(e);
//        }
//    }
//
    public static class SaveRelationshipException extends Throwable {
        SaveRelationshipException(IOException e) {
            super(e);
        }
    }
//
//    public class DeleteEncounterException extends Throwable {
//        DeleteEncounterException(IOException e) {
//            super(e);
//        }
//    }
}
