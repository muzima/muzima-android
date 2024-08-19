package com.muzima.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.muzima.db.entities.User;

public class OpenMRSSession {
    @SerializedName("sessionId")
    @Expose
    private String sessionId;

    @SerializedName("authenticated")
    @Expose
    private Boolean isAuthenticated;

    @SerializedName("user")
    @Expose
    private User user;

    public OpenMRSSession(String sessionId, Boolean isAuthenticated, User user) {
        this.sessionId = sessionId;
        this.isAuthenticated = isAuthenticated;
        this.user = user;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getAuthenticated() {
        return isAuthenticated;
    }

    public Boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        isAuthenticated = authenticated;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
