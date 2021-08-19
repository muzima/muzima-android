package com.muzima.model.events;

public class CloseSingleFormEvent {
    private int position;

    public CloseSingleFormEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
