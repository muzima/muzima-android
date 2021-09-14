/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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
