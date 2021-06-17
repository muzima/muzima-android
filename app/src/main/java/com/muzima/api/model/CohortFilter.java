package com.muzima.api.model;

public class CohortFilter {
    private Cohort cohort;
    private boolean selected;

    public CohortFilter(Cohort cohort, boolean selected) {
        this.cohort = cohort;
        this.selected = selected;
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
