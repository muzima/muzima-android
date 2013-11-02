package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import com.muzima.api.model.Concept;
import com.muzima.utils.Constants;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.muzima.utils.Constants.CONCEPT_PREF;
import static com.muzima.utils.Constants.CONCEPT_PREF_KEY;

public class ConceptPreferenceService extends PreferenceService {

    public ConceptPreferenceService(Context context) {
        super(context);
    }

    public void addConcepts(List<Concept> concepts) {
        Set<String> copyOfUuidSet = new LinkedHashSet<String>(getConcepts());
        for (Concept concept : concepts) {
            copyOfUuidSet.add(concept.getUuid());
        }
        saveConcepts(copyOfUuidSet);
    }

    public void removeConcept(Concept concept) {
        Set<String> concepts = new HashSet<String>(getConcepts());
        concepts.remove(concept.getUuid());
        saveConcepts(concepts);
    }

    public List<String> getConcepts(){
        SharedPreferences cohortSharedPref = context.getSharedPreferences(Constants.CONCEPT_PREF, android.content.Context.MODE_PRIVATE);
        return deserialize(cohortSharedPref.getString(CONCEPT_PREF_KEY, null));
    }

    private void saveConcepts(Set<String> copyOfUuidSet) {
        SharedPreferences conceptSharedPreferences = context.getSharedPreferences(CONCEPT_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = conceptSharedPreferences.edit();
        putStringSet(CONCEPT_PREF_KEY, copyOfUuidSet, editor);
        editor.commit();
    }
}
