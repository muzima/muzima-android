package com.muzima.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PersonAttribute {
    @PrimaryKey
    @NonNull
    private String attributeUuid;
    private int personId;
    private String attributeValue;
    private int attributeTypeId;

    @NonNull
    public String getAttributeUuid() {
        return attributeUuid;
    }

    public void setAttributeUuid(@NonNull String attributeUuid) {
        this.attributeUuid = attributeUuid;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public int getAttributeTypeId() {
        return attributeTypeId;
    }

    public void setAttributeTypeId(int attributeTypeId) {
        this.attributeTypeId = attributeTypeId;
    }
}
