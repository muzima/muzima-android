package com.muzima.model.events;

public class BottomSheetToggleEvent {
    private int state;
    public BottomSheetToggleEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
