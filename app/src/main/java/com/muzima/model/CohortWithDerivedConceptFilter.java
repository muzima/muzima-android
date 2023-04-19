package com.muzima.model;

import com.muzima.api.model.Cohort;
import com.muzima.api.model.DerivedConcept;

public class CohortWithDerivedConceptFilter {
    private Cohort cohort;
    private String derivedConceptUuid;
    private String derivedObservationFilter;

    public CohortWithDerivedConceptFilter(Cohort cohort,String derivedConceptUuid, String derivedObservationFilter){
        this.cohort = cohort;
        this.derivedConceptUuid = derivedConceptUuid;
        this.derivedObservationFilter = derivedObservationFilter;
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
}
