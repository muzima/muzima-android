package com.muzima.model.events;

import com.muzima.api.model.Cohort;

public class CohortFilterActionEvent {
    private Cohort filter;
    private boolean noSelectionEvent;

    public CohortFilterActionEvent(Cohort filter, boolean noSelectionEvent) {
        this.filter = filter;
        this.noSelectionEvent = noSelectionEvent;
    }

    public Cohort getFilter() {
        return filter;
    }

    public void setFilter(Cohort filter) {
        this.filter = filter;
    }

    public boolean isNoSelectionEvent() {
        return noSelectionEvent;
    }

    public void setNoSelectionEvent(boolean noSelectionEvent) {
        this.noSelectionEvent = noSelectionEvent;
    }
}
