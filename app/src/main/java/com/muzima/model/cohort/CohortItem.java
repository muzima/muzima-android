package com.muzima.model.cohort;

import com.muzima.api.model.Cohort;

public class CohortItem {
    private Cohort cohort;
    private boolean selected;
    public CohortItem(Cohort cohort, boolean selected) {
        this.cohort = cohort;
        this.selected = selected;
    }

    public CohortItem(Cohort cohort) {
        this.cohort = cohort;
        this.selected = false;
    }

    public Cohort getCohort() {
        return cohort;
    }

    public void setCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
