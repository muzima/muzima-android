/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model;

public class CohortFilter {
    private CohortWithFilter cohortWithFilter;
    private boolean selected;
    private boolean checkboxPadded;

    public CohortFilter(CohortWithFilter cohortWithFilter, boolean selected) {
        this.cohortWithFilter = cohortWithFilter;
        this.selected = selected;
    }

    public boolean isCheckboxPadded() {
        return checkboxPadded;
    }

    public void setCheckboxPadded(boolean checkboxPadded) {
        this.checkboxPadded = checkboxPadded;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public CohortWithFilter getCohortWithFilter() {
        return cohortWithFilter;
    }

    public void setCohortWithFilter(CohortWithFilter cohortWithFilter) {
        this.cohortWithFilter = cohortWithFilter;
    }
}
