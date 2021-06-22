package com.muzima.model.events;

public class FormSearchEvent {
    private String searchTerm;
    private int page;
    public FormSearchEvent(String searchTerm, int page) {
        this.searchTerm = searchTerm;
        this.page = page;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
