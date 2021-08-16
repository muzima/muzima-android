package com.muzima.model.events;

public class FormSortEvent {
    private int sortingStrategy;

    public FormSortEvent(int sortingStrategy) {
        this.sortingStrategy = sortingStrategy;
    }

    public int getSortingStrategy() {
        return sortingStrategy;
    }

    public void setSortingStrategy(int sortingStrategy) {
        this.sortingStrategy = sortingStrategy;
    }
}
