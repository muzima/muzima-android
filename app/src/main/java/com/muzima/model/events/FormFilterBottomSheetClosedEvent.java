package com.muzima.model.events;

public class FormFilterBottomSheetClosedEvent {
    private boolean closeEvent;

    public FormFilterBottomSheetClosedEvent(boolean closeEvent) {
        this.closeEvent = closeEvent;
    }

    public boolean isCloseEvent() {
        return closeEvent;
    }

    public void setCloseEvent(boolean closeEvent) {
        this.closeEvent = closeEvent;
    }
}
