package com.muzima.model.events;

import com.muzima.model.CohortFilter;

import java.util.List;

public class CohortFilterActionEvent {
    private List<CohortFilter> filters;
    private boolean noSelectionEvent;

    public CohortFilterActionEvent(List<CohortFilter> filters, boolean noSelectionEvent) {
        this.filters = filters;
        this.noSelectionEvent = noSelectionEvent;
    }

    public List<CohortFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<CohortFilter> filters) {
        this.filters = filters;
    }

    public boolean isNoSelectionEvent() {
        return noSelectionEvent;
    }

    public void setNoSelectionEvent(boolean noSelectionEvent) {
        this.noSelectionEvent = noSelectionEvent;
    }
}
