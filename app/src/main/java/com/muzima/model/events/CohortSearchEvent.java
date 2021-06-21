package com.muzima.model.events;

public class CohortSearchEvent {
    private String searchTerm;
    public CohortSearchEvent(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}
