/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model.relationship;

import androidx.annotation.NonNull;
import com.muzima.api.model.RelationshipType;

public class RelationshipTypeWrap implements Comparable<RelationshipTypeWrap> {
    private String uuid;
    private String name;
    private String side;
    private RelationshipType relationshipType;

    public RelationshipTypeWrap(String uuid, String name, String side, RelationshipType relationshipType) {
        this.uuid = uuid;
        this.name = name;
        this.side = side;
        this.relationshipType = relationshipType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    @Override
    public int compareTo(@NonNull RelationshipTypeWrap relationshipTypeWrap) {
        if (this.getName() != null && relationshipTypeWrap.getName() != null) {
            return this.getName().toLowerCase().compareTo(relationshipTypeWrap.getName().toLowerCase());
        }
        return 0;
    }
}
