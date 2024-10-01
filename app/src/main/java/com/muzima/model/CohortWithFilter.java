/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model;

import com.muzima.api.model.Cohort;

public class CohortWithFilter {
    private Cohort cohort;
    private String derivedConceptUuid;
    private String derivedObservationFilter;
    private String conceptUuid;
    private String observationFilter;

    public CohortWithFilter(Cohort cohort,String derivedConceptUuid, String derivedObservationFilter, String conceptUuid, String observationFilter){
        this.cohort = cohort;
        this.derivedConceptUuid = derivedConceptUuid;
        this.derivedObservationFilter = derivedObservationFilter;
        this.conceptUuid = conceptUuid;
        this.observationFilter = observationFilter;
    }

    public Cohort getCohort() {
        return cohort;
    }

    public void setCohort(Cohort cohort) {
        this.cohort = cohort;
    }


    public String getDerivedObservationFilter() {
        return derivedObservationFilter;
    }

    public void setDerivedObservationFilter(String derivedObservationFilter) {
        this.derivedObservationFilter = derivedObservationFilter;
    }

    public String getDerivedConceptUuid() {
        return derivedConceptUuid;
    }

    public void setDerivedConceptUuid(String derivedConceptUuid) {
        this.derivedConceptUuid = derivedConceptUuid;
    }

    public String getConceptUuid() {
        return conceptUuid;
    }

    public void setConceptUuid(String conceptUuid) {
        this.conceptUuid = conceptUuid;
    }

    public String getObservationFilter() {
        return observationFilter;
    }

    public void setObservationFilter(String observationFilter) {
        this.observationFilter = observationFilter;
    }
}
