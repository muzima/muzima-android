package com.muzima.model.events;

import com.muzima.model.cohort.CohortItem;

import java.util.List;

public class CohortsActionModeEvent {
    private List<CohortItem> selectedCohorts;
    public CohortsActionModeEvent(List<CohortItem> selectedCohorts) {
        this.selectedCohorts = selectedCohorts;
    }

    public List<CohortItem> getSelectedCohorts() {
        return selectedCohorts;
    }

    public void setSelectedCohorts(List<CohortItem> selectedCohorts) {
        this.selectedCohorts = selectedCohorts;
    }
}
