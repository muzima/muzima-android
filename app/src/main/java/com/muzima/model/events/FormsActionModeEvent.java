/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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
