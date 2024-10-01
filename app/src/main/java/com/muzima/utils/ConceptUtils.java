/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import com.muzima.api.model.ConceptName;
import com.muzima.api.model.DerivedConceptName;

import java.util.List;

public class ConceptUtils {
    public static String getConceptNameFromConceptNamesByLocale(List<ConceptName> conceptNames, String preferredLocale){

        if(conceptNames.size()==1){
            return conceptNames.get(0).getName();
        }

        String preferredForLocale = null;
        String conceptForLocale = null;
        String anyPreferredConcept = null;
        String anyOtherConcept = null;

        for(ConceptName conceptName : conceptNames){
            if(conceptName.getLocale().equals(preferredLocale) && conceptName.isPreferred()){
                preferredForLocale = conceptName.getName();
            }

            if(conceptName.getLocale().equals(preferredLocale) ){
                conceptForLocale = conceptName.getName();
            }

            if(!conceptName.getLocale().equals(preferredLocale) && conceptName.isPreferred()){
                anyPreferredConcept = conceptName.getName();
            }

            if(!conceptName.getLocale().equals(preferredLocale) && !conceptName.isPreferred()){
                anyOtherConcept = conceptName.getName();
            }
        }

        if(preferredForLocale != null){
            return preferredForLocale;
        }else if(conceptForLocale != null){
            return conceptForLocale;
        }else if(anyPreferredConcept != null){
            return anyPreferredConcept;
        }

        return anyOtherConcept;
    }

    public static String getDerivedConceptNameFromConceptNamesByLocale(List<DerivedConceptName> derivedConceptNames, String preferredLocale){

        if(derivedConceptNames.size()==1){
            return derivedConceptNames.get(0).getFullName();
        }

        String derivedConceptNameForLocale = null;
        String otherDerivedConceptName = null;

        for(DerivedConceptName derivedConceptName : derivedConceptNames){
            if(derivedConceptName.getLocale().equals(preferredLocale) ){
                derivedConceptNameForLocale = derivedConceptName.getFullName();
            }

            if(!derivedConceptName.getLocale().equals(preferredLocale)){
                otherDerivedConceptName = derivedConceptName.getFullName();
            }
        }

        if(derivedConceptNameForLocale != null){
            return derivedConceptNameForLocale;
        }else if(otherDerivedConceptName != null){
            return otherDerivedConceptName;
        }

        return otherDerivedConceptName;
    }
}