/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.api.service.RelationshipService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    /********************************************************************************************************
     *                               METHODS FOR RELATIONSHIP TYPES
     *********************************************************************************************************
     * Download all {@link RelationshipType} from the server
     * @return List of relationship types
     * @throws RetrieveRelationshipTypeException RelationshipType Retrieval Exception
     */
    public List<RelationshipType> downloadAllRelationshipTypes() throws RetrieveRelationshipTypeException {
        try {
            return relationshipService.downloadAllRelationshipTypes();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while downloading Relationship Types from server", e);
            throw new RetrieveRelationshipTypeException(e);
        }
    }

    /**
     *  Saves a single relationship type to the local repository
     * @param relationshipType an instance of {@link RelationshipType} to save
     * @throws SaveRelationshipTypeException {@link RelationshipType} save exception
     */
    public void saveRelationshipType(RelationshipType relationshipType) throws SaveRelationshipTypeException {
        try {
            relationshipService.saveRelationshipType(relationshipType);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the relationship type", e);
            throw new SaveRelationshipTypeException(e);
        }
    }

    /**
     *  Saves a List of relationship types to the local repository
     * @param relationshipTypes list of {@link RelationshipType} to save
     * @throws SaveRelationshipTypeException {@link RelationshipType} save exception
     */
    public void saveRelationshipTypes(List<RelationshipType> relationshipTypes) throws SaveRelationshipTypeException {
        try {
            relationshipService.saveRelationshipTypes(relationshipTypes);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the relationship types list", e);
            throw new SaveRelationshipTypeException(e);
        }
    }

    /**
     * Retrieve all relationship types from the local repository
     * @return a list of relationship types
     * @throws RetrieveRelationshipTypeException RelationshipType Retrieval Exception
     */
    public List<RelationshipType>  getAllRelationshipTypes() throws RetrieveRelationshipTypeException{
        try {
            return relationshipService.getAllRelationshipTypes();
        } catch (IOException e) {
            throw new RetrieveRelationshipTypeException(e);
        }
    }

    /**
     * Retrieve a single Relationship Type from the local repository
     * @param uuid Uuid for a {@link RelationshipType} object
     * @return a single {@link RelationshipType}
     * @throws RetrieveRelationshipTypeException RelationshipType Retrieval Exception
     */
    public RelationshipType  getRelationshipTypeByUuid(String uuid) throws RetrieveRelationshipTypeException{
        try {
            return relationshipService.getRelationshipTypeByUuid(uuid);
        } catch (IOException e) {
            throw new RetrieveRelationshipTypeException(e);
        }
    }

    private void saveRelationshipTypesFromRelationships(List<Relationship> relationships) {

        // if the relationship has a new type we save it to the local repo
        try{
            for (Relationship relationship : relationships) {
                if (getRelationshipTypeByUuid(relationship.getRelationshipType().getUuid()) == null) {
                    saveRelationshipType(relationship.getRelationshipType());
                }
            }
        } catch (RetrieveRelationshipTypeException | SaveRelationshipTypeException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the relationship type list while saving relationship", e);
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

    /**
     * Save a list of relationships to the local repo
     * @param relationships list of {@link Relationship}
     * @throws SaveRelationshipException Relationship Save Exception
     */
    public void saveRelationships(List<Relationship> relationships) throws SaveRelationshipException {
        try {
            relationshipService.saveRelationships(relationships);

            saveRelationshipTypesFromRelationships(relationships);
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
