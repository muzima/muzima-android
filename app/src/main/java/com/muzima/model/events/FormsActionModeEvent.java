package com.muzima.model.events;

import com.muzima.api.model.Form;

import java.util.List;

public class FormsActionModeEvent {
    private List<Form> selectedFormsList;

    public FormsActionModeEvent(List<Form> selectedFormsList) {
        this.selectedFormsList = selectedFormsList;
    }

    public List<Form> getSelectedFormsList() {
        return selectedFormsList;
    }

    public void setSelectedFormsList(List<Form> selectedFormsList) {
        this.selectedFormsList = selectedFormsList;
    }
}
