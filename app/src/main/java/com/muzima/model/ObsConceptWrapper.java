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

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;

import java.util.ArrayList;
import java.util.List;

public class ObsConceptWrapper {
    private Concept concept;
    private List<Observation> getMatchingObs;

    public ObsConceptWrapper(Concept concept, List<Observation> getMatchingObs) {
        this.concept = concept;
        this.getMatchingObs = setObsType(concept, getMatchingObs);
    }

    public Concept getConcept() {
        return concept;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    public List<Observation> getMatchingObs() {
        return getMatchingObs;
    }

    public void setMatchingConcepts(List<Observation> matchingConcepts) {
        this.getMatchingObs = matchingConcepts;
    }

    public List<Observation> setObsType(Concept concept,List<Observation> getMatchingObs){
        List<Observation> obsList = new ArrayList<>();
        for(Observation obs : getMatchingObs){
            obs.setConcept(concept);
            obsList.add(obs);
        }
        return obsList;
    }
}
