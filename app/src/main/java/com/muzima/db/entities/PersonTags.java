package com.muzima.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PersonTags {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;
    private int personId;
    private String personTagUuid;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public String getPersonTagUuid() {
        return personTagUuid;
    }

    public void setPersonTagUuid(String personTagUuid) {
        this.personTagUuid = personTagUuid;
    }
}
