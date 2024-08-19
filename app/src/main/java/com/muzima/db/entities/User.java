package com.muzima.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.muzima.api.model.Patient;
import com.muzima.db.DateConverter;

import java.util.Date;

@Entity
public class User {
    @PrimaryKey
    @NonNull
    @SerializedName("uuid")
    @Expose
    private String uuid;

    @SerializedName("username")
    @Expose
    private String username;

    @SerializedName("systemId")
    @Expose
    private String systemId;

    public User(String uuid, String username, String systemId) {
        this.uuid = uuid;
        this.username = username;
        this.systemId = systemId;
    }

    @TypeConverters(DateConverter.class)
    private Date dateLastLoggedIn;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public Date getDateLastLoggedIn() {
        return dateLastLoggedIn;
    }

    public void setDateLastLoggedIn(Date dateLastLoggedIn) {
        this.dateLastLoggedIn = dateLastLoggedIn;
    }

    @NonNull
    public String getUuid() {
        return uuid;
    }

    public void setUuid(@NonNull String uuid) {
        this.uuid = uuid;
    }
}
