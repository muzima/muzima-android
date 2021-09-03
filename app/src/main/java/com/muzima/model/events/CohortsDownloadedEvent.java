package com.muzima.model.events;

public class CohortsDownloadedEvent {
    private boolean success;

    public CohortsDownloadedEvent(boolean status) {
        this.success = status;
    }

    public boolean getSuccess() {
        return success;
    }
}
