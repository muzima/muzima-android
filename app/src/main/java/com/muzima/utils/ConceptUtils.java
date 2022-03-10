package com.muzima.utils;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.ConceptName;

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
}
