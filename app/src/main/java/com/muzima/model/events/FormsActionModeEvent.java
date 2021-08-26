package com.muzima.model.events;

import com.muzima.api.model.Form;

import java.util.List;

public class FormsActionModeEvent {
    private List<Form> selectedFormsList;
    private boolean formSelected;

    public FormsActionModeEvent(List<Form> selectedFormsList, boolean actionModeActive) {
        this.selectedFormsList = selectedFormsList;
        this.formSelected = actionModeActive;
    }

    public List<Form> getSelectedFormsList() {
        return selectedFormsList;
    }

    public boolean getFormSelected() {
        return formSelected;
    }

    public void setSelectedFormsList(List<Form> selectedFormsList) {
        this.selectedFormsList = selectedFormsList;
    }
}
