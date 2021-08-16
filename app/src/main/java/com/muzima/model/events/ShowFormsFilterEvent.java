package com.muzima.model.events;

public class ShowFormsFilterEvent {
    private int activeFilter;
    private boolean closeAction;

    public ShowFormsFilterEvent(boolean closeAction) {
        this.closeAction = closeAction;
    }

    public ShowFormsFilterEvent(int activeFilter) {
        this.activeFilter = activeFilter;
    }

    public int getActiveFilter() {
        return activeFilter;
    }

    public void setActiveFilter(int activeFilter) {
        this.activeFilter = activeFilter;
    }

    public boolean isCloseAction() {
        return closeAction;
    }

    public void setCloseAction(boolean closeAction) {
        this.closeAction = closeAction;
    }
}

