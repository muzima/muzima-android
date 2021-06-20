package com.muzima.api.model;

public class CohortFilter {
    private Cohort cohort;
    private boolean selected;
    private boolean checkboxPadded;

    public CohortFilter(Cohort cohort, boolean selected) {
        this.cohort = cohort;
        this.selected = selected;
    }

    public boolean isCheckboxPadded() {
        return checkboxPadded;
    }

    public void setCheckboxPadded(boolean checkboxPadded) {
        this.checkboxPadded = checkboxPadded;
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
