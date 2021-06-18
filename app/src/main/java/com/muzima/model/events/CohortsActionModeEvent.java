package com.muzima.model.events;

import com.muzima.api.model.Cohort;

import java.util.List;

public class CohortsActionModeEvent {
    private List<Cohort> selectedCohorts;
    public CohortsActionModeEvent(List<Cohort> selectedCohorts) {
        this.selectedCohorts = selectedCohorts;
    }

    public List<Cohort> getSelectedCohorts() {
        return selectedCohorts;
    }

    public void setSelectedCohorts(List<Cohort> selectedCohorts) {
        this.selectedCohorts = selectedCohorts;
    }
}
