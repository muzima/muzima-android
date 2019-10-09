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
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.api.service.RelationshipService;

import java.io.IOException;
import java.util.List;

public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    /********************************************************************************************************
    *                               METHODS FOR RELATIONSHIP TYPES
    *********************************************************************************************************/

    public List<RelationshipType> downloadAllRelationshipTypes() throws RetrieveRelationshipTypeException {
        try {
            return relationshipService.downloadAllRelationshipTypes();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while downloading Relationship Types from server", e);
            throw new RetrieveRelationshipTypeException(e);
        }
    }

    public void saveRelationshipTypes(List<RelationshipType> relationshipTypes) throws SaveRelationshipTypeException {
        try {
            relationshipService.saveRelationshipTypes(relationshipTypes);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the relationship types list", e);
            throw new SaveRelationshipTypeException(e);
        }
    }

    public List<RelationshipType>  getAllRelationshipTypes() throws RetrieveRelationshipTypeException{
        try {
            return relationshipService.getAllRelationshipTypes();
        } catch (IOException e) {
            throw new RetrieveRelationshipTypeException(e);
        }
    }

    public RelationshipType  getRelationshipTypeByUuid(String uuid) throws RetrieveRelationshipTypeException{
        try {
            return relationshipService.getRelationshipTypeByUuid(uuid);
        } catch (IOException e) {
            throw new RetrieveRelationshipTypeException(e);
        }
    }

    /********************************************************************************************************
     *                               METHODS FOR RELATIONSHIPS
     *********************************************************************************************************/
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

    public List<Relationship>  getRelationshipsForPerson(String personUuid) throws RetrieveRelationshipException{
        try {
            return relationshipService.getRelationshipsForPerson(personUuid);
        } catch (IOException e) {
            throw new RetrieveRelationshipException(e);
        }
    }

    /********************************************************************************************************
     *                               METHODS FOR EXCEPTION HANDLING
     *********************************************************************************************************/

    public static class RetrieveRelationshipException extends Throwable {
        RetrieveRelationshipException(IOException e) {
            super(e);
        }
    }

    public static class RetrieveRelationshipTypeException extends Throwable {
        RetrieveRelationshipTypeException(IOException e) {
            super(e);
        }
    }

    public static class SaveRelationshipException extends Throwable {
        SaveRelationshipException(IOException e) {
            super(e);
        }
    }

    public static class SaveRelationshipTypeException extends Throwable {
        SaveRelationshipTypeException(IOException e) {
            super(e);
        }
    }
}
